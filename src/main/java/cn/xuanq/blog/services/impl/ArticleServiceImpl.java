package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.ArticleDao;
import cn.xuanq.blog.dao.ArticleNoContentDao;
import cn.xuanq.blog.dao.CommentDao;
import cn.xuanq.blog.dao.LabelDao;
import cn.xuanq.blog.pojo.*;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.IArticleService;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.IdWorker;
import cn.xuanq.blog.utils.RedisUtil;
import cn.xuanq.blog.utils.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.*;

@Slf4j
@Service
@Transactional
public class ArticleServiceImpl extends BaseService implements IArticleService {

    @Autowired
    private IUserService userService;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private ArticleDao articleDao;

    @Autowired
    private ArticleNoContentDao articleNoContentDao;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Gson gson;

    /**
     * 后期定时发布
     * 如果是多人博客系统，得考虑审核问题-->成功，通知，审核不通过，也可通知
     * 保存成草稿
     * 1、用户手动提交： 发生页面跳转 --> 提交完即可
     * 2、代码自动提交，每隔一段时间就会提交  --> 不会发生跳转 --> 多次提交 --> 如果没有唯一标识，就会重复添加到数据库里
     *
     * 不管是那种提交-->必须要有标题
     * <p>
     * 方案一:每次用户发新文章之前-->先向后台请求一个唯一文章ID
     * 如果是更新文件,则不需要请求这个唯一的ID
     *<p>
     * 方案二:可以直接提交，后台判断有没有ID,如果没有ID，就新创建，并且ID作为此次返回的结果
     * 如果有ID，就修改已经存在的内容。
     *
     * <p>
     * 推荐做法:
     * 自动保存草稿，在前端本地完成，也就是保存在本地。
     * 如果是用户手动提交的，就提交到后台
     *
     * 防止重复提交(网络卡顿的时候，用户点了几次提交):
     * 可以通过ID的方式
     * 通过token_key的提交频率来计算，如果30秒之内有多次提交，只有最前的一次有效
     *其他的提交直接return，提示用户不要太频繁操作
     * <p>
     * 前端的处理:点击了提交以后，禁止按钮可以使用，等到有响应结果，再改变按钮的状态
     *
     * @param article
     * @return
     */
    @Override
    public ResponseResult postArticle(Article article) {
        // 检查用户，获取用户对象
        InisUser inisUser = userService.checkInisUser();
        // 未登录
        if (inisUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        // 检查数据
        // title、分类ID、内容、类型、摘要、标签
        String title = article.getTitle();
        if (TextUtils.isEmpty(title)) {
            return ResponseResult.FAILED("标题不可以为空");
        }

        // 2种 草稿和发布
        String state = article.getState();
        if (!Constants.Article.STATE_PUBLISH.equals(state) && !Constants.Article.STATE_DRAFT.equals(state)) {
            // 不支持此操作
            return ResponseResult.FAILED("不支持此操作");
        }

        String type = article.getType();
        if (TextUtils.isEmpty(type)) {
            return ResponseResult.FAILED("类型不可以为空");
        }
        if (!"0".equals(type) && !"1".equals(type)) {
            return ResponseResult.FAILED("类型格式不对");
        }

        //以下检查是发布的检查，草稿不需要检查
        if (Constants.Article.STATE_PUBLISH.equals(state)) {
            if (title.length() > Constants.Article.TITLE_MAX_LENGTH) {
                return ResponseResult.FAILED("文章标题不可以超过" + Constants.Article.TITLE_MAX_LENGTH + "个字符");
            }
            String content = article.getContent();
            if (TextUtils.isEmpty(content)) {
                return ResponseResult.FAILED("内容不可以为空");
            }
            String summary = article.getSummary();
            if (TextUtils.isEmpty(summary)) {
                return ResponseResult.FAILED("摘要不可以为空");
            }
            if (summary.length() > Constants.Article.SUMMARY_MAX_LENGTH) {
                return ResponseResult.FAILED("摘要不可以超出" + Constants.Article.SUMMARY_MAX_LENGTH + "个字符");
            }
            // 标签-标签1-标签2
            String labels = article.getLabel();
            if (TextUtils.isEmpty(labels)) {
                return ResponseResult.FAILED("标签不可以为空");
            }
        }

        String articleId = article.getId();
        if (TextUtils.isEmpty(articleId)) {
            //新内容，数据库里没有的
            // 补充数据，ID、创建时间、用户ID、更新时间
            article.setId(idWorker.nextId() + "");
            article.setCreateTime(new Date());
        } else {
            // 更新内容,对状态进行处理，如果是已经发布的，则不能在保存为草稿
            Article articleFromDb = articleDao.findOneById(articleId);
            if (Constants.Article.STATE_PUBLISH.equals(articleFromDb.getState()) &&
                    Constants.Article.STATE_DRAFT.equals(state)) {
                // 已经发布，只能更新，不能保存草稿
                return ResponseResult.FAILED("已将发布的文章不支持保存为草稿");
            }
        }
        article.setUserId(inisUser.getId());
        article.setUpdateTime(new Date());
        // 保存到数据库里
        articleDao.save(article);
        //TODO:保存到搜索的数据库里
        // 打散标签，入库，统计
        String labels = article.getLabel();
        if (!TextUtils.isEmpty(labels)) {
            this.setupLabels(labels);
        }
        //        删除文章列表
        redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
        // 返回结果,只有一种case使用到这个ID
        // 如果要做程序自动保存成草稿（比如说每30秒保存一次，就需要加上这个ID了，否则会创建多个Item)
        return ResponseResult.SUCCESS(Constants.Article.STATE_DRAFT.equals(state) ? "草稿保存成功" : "文章发表成功").setData(article.getId());
    }

    @Autowired
    private LabelDao labelDao;

    private void setupLabels(String labels) {
        List<String> labelList = new ArrayList<>();
        if (labels.contains("-")) {
            labelList.addAll(Arrays.asList(labels.split("-")));
        } else {
            labelList.add(labels);
        }
        // 入库，统计
        for (String label:
             labelList) {
            // 找出来
            // Label targetLabel = labelDao.findOneByName(label);
            // if (targetLabel == null) {
            //     targetLabel = new Label();
            //     targetLabel.setId(idWorker.nextId() + "");
            //     targetLabel.setCount(0);
            //     targetLabel.setName(label);
            //     targetLabel.setCreateTime(new Date());
            // }
            // long count = targetLabel.getCount();
            // targetLabel.setCount(++count);
            // targetLabel.setUpdateTime(new Date());
            int result = labelDao.updateCountByName(label);
            if (result == 0) {
                Label targetLabel = new Label();
                targetLabel.setId(idWorker.nextId() + "");
                targetLabel.setCount(1);
                targetLabel.setName(label);
                targetLabel.setCreateTime(new Date());
                targetLabel.setUpdateTime(new Date());
                labelDao.save(targetLabel);
            }
        }
    }

    /**
     * 管理中心，获取文章列表
     * @param page          页码
     * @param size          每一页数量
     * @param keyword       搜索关键字
     * @param categoryId    分类ID
     * @param state         状态：已经删除的、草稿、已经发布的、置顶的
     * @return
     */
    @Override
    public ResponseResult listArticles(int page, int size, String keyword, String categoryId, String state) {
        // 处理一下page和size
        page = checkPage(page);
        size = checkSize(size);
        // 做第一页数据缓存
        String articleListJson = (String) redisUtil.get(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
        boolean isSearch = !TextUtils.isEmpty(keyword) || !TextUtils.isEmpty(categoryId) || !TextUtils.isEmpty(state);
        if (!TextUtils.isEmpty(articleListJson) && page == 1 && !isSearch) {
            PageList<ArticleNoContent> result = gson.fromJson(articleListJson, new TypeToken<PageList<ArticleNoContent>>() {
            }.getType());
            log.info("article list first page from redis");
            return ResponseResult.SUCCESS("获取文章列表成功").setData(result);
        }
        // 创建分页和排序条件
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        // 开始查询
        Page<ArticleNoContent> all = articleNoContentDao.findAll(new Specification<ArticleNoContent>() {
            @Override
            public Predicate toPredicate(Root<ArticleNoContent> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                // 判断是否有传参数
                if (!TextUtils.isEmpty(state)) {
                    Predicate statePre = cb.equal(root.get("state").as(String.class), state);
                    predicates.add(statePre);
                }
                if (!TextUtils.isEmpty(categoryId)) {
                    Predicate categoryIdPre = cb.equal(root.get("categoryId").as(String.class), categoryId);
                    predicates.add(categoryIdPre);
                }
                if (!TextUtils.isEmpty(keyword)) {
                    Predicate titlePre = cb.like(root.get("title").as(String.class), "%"+ keyword + "%");
                    predicates.add(titlePre);
                }
                Predicate[] preArray = new Predicate[predicates.size()];
                predicates.toArray(preArray);
                return cb.and(preArray);
            }
        }, pageable);

        // 处理查询条件
        PageList<ArticleNoContent> result = new PageList<>();
        // 解析page
        result.parsePage(all);
        // 保存到redis
        if (page == 1 && !isSearch) {
            redisUtil.set(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE, gson.toJson(result), Constants.TimeValueInSecond.MIN_15);
        }
        return ResponseResult.SUCCESS("获取文章列表成功").setData(result);
    }

    /**
     * 如果有审核机制:审核中的文章-->只有管理员和作者自己可以获取
     * 有草稿，删除的，置顶的，已经发布的
     * 删除的不能获取，其他的都能获取
     * @param articleId
     * @return
     */
    @Override
    public ResponseResult getArticleById(String articleId) {
        // 查询出文章
        Article article = articleDao.findOneById(articleId);
        if (article == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        // 判断文章状态
        String state = article.getState();
        if (Constants.Article.STATE_PUBLISH.equals(state) || Constants.Article.STATE_TOP.equals(state)) {
            // 可以返回
            return ResponseResult.SUCCESS("获取文章成功").setData(article);
        }
        // 如果是删除/草稿，需要管理员角色
        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null || !Constants.User.ROLE_ADMIN.equals(inisUser.getRoles())) {
            return ResponseResult.PERMISSION_DENIED();
        }
        // 返回结果
        return ResponseResult.SUCCESS("获取文章成功").setData(article);
    }

    /**
     * 更新文章内容
     * 该接口只支持修改内容：标题，内容，标签，分类，摘要
     * @param articleId
     * @param article
     * @return
     */
    @Override
    public ResponseResult updateArticle(String articleId, Article article) {
        // 先找出来
        Article articleFromDb = articleDao.findOneById(articleId);
        if (articleFromDb == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        // 内容修改
        String title = article.getTitle();
        if (!TextUtils.isEmpty(title)) {
            articleFromDb.setTitle(title);
        }
        String summary = article.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            articleFromDb.setSummary(summary);
        }
        String content = article.getContent();
        if (!TextUtils.isEmpty(content)) {
            articleFromDb.setContent(content);
        }
        String label = article.getLabel();
        if (!TextUtils.isEmpty(label)) {
            articleFromDb.setLabel(label);
        }
        String categoryId = article.getCategoryId();
        if (!TextUtils.isEmpty(categoryId)) {
            articleFromDb.setCategoryId(categoryId);
        }
        articleFromDb.setCover(article.getCover());
        articleFromDb.setUpdateTime(new Date());
        articleDao.save(articleFromDb);
        redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
        // 返回结果
        return ResponseResult.SUCCESS("文章更新成功");
    }

    @Autowired
    private CommentDao commentDao;
    /**
     * 删除文章 物理删除
     * 数据库 评论外键 无法删除
     * @param articleId
     * @return
     */
    @Override
    public ResponseResult deleteArticleById(String articleId) {
//        要先把评论也删除了
        commentDao.deleteAllByArticleId(articleId);
//        外键是article的Id
        int result = articleDao.deleteAllById(articleId);
        if (result > 0) {
            redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            return ResponseResult.SUCCESS("文章删除成功");
        }
        return ResponseResult.FAILED("文章不存在");
    }

    /**
     * 通过修改状态删除文章，标记删除
     * @param articleId
     * @return
     */
    @Override
    public ResponseResult deleteArticleByUpdateState(String articleId) {
        int result = articleDao.deleteArticleByState(articleId);
        if (result > 0) {
            redisUtil.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            return ResponseResult.SUCCESS("文章删除成功");
        }
        return ResponseResult.FAILED("文章不存在");
    }

    @Override
    public ResponseResult topArticle(String articleId) {
        // 必须是已经发布的，置顶
        Article article = articleDao.findOneById(articleId);
        if (article == null) {

        }
        String state = article.getState();
        if (Constants.Article.STATE_PUBLISH.equals(state)) {
            // 如果已经置顶，就取消置顶
            article.setState(Constants.Article.STATE_TOP);
            articleDao.save(article);
            return ResponseResult.SUCCESS("置顶成功");
        }
        if (Constants.Article.STATE_TOP.equals(state)) {
            // 如果已经置顶，就取消置顶
            article.setState(Constants.Article.STATE_PUBLISH);
            articleDao.save(article);
            return ResponseResult.SUCCESS("取消置顶成功");
        }
        return ResponseResult.FAILED("不支持该操作");
    }

    /**
     * 获取置顶文章
     * 跟权限无关
     * 状态必须置顶
     * @return
     */
    @Override
    public ResponseResult listTopArticles() {
        List<Article> result = articleDao.findAll(new Specification<Article>() {
            @Override
            public Predicate toPredicate(Root<Article> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.equal(root.get("state").as(String.class), Constants.Article.STATE_TOP);
            }
        });
        return ResponseResult.SUCCESS("获取置顶文章成功").setData(result);
    }

    @Autowired
    private Random random;

    /**
     * 获取推荐文章，通过标签来计算
     * @param articleId
     * @param size
     * @return
     */
    @Override
    public ResponseResult listRecommendArticle(String articleId, int size) {
        // 查询文章
        String labels = articleDao.listArticleLabelById(articleId);
        // 打散标签
        List<String> labelList = new ArrayList<>();
        if (!labels.contains("-")) {
            labelList.add(labels);
        } else {
            labelList.addAll(Arrays.asList(labels.split("-")));
        }
        // 从列表中随即获取一标签，查询与此标签相似的文章
        String targetLabel = labelList.get(random.nextInt(labelList.size()));
        log.info("targetLabel --> " + targetLabel);
        List<ArticleNoContent> likeResultList = articleNoContentDao.listArticleByLikeLabel("%" + targetLabel + "%", articleId, size);
        // 判断它的长度
        if (likeResultList.size() < size) {
            // 说明不够数量，获取最新的文章作为补充
            int dxSize = size - likeResultList.size();
            List<ArticleNoContent> dxList = articleNoContentDao.listLastArticleBySize(articleId, dxSize);
            // 这个写法有一定弊端，会把可能前面找到的也加进来，概率比较小，如果文章比较多
            likeResultList.addAll(dxList);
        }
        return ResponseResult.SUCCESS("获取推荐文章成功").setData(likeResultList);
    }

    @Override
    public ResponseResult listArticleByLabel(int page, int size, String label) {
        page = checkPage(page);
        size = checkSize(size);
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<ArticleNoContent> all = articleNoContentDao.findAll(new Specification<ArticleNoContent>() {
            @Override
            public Predicate toPredicate(Root<ArticleNoContent> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                Predicate labelPre = criteriaBuilder.like(root.get("labels").as(String.class), "%" + label + "%");
                Predicate statePublishPre = criteriaBuilder.equal(root.get("state").as(String.class), Constants.Article.STATE_PUBLISH);
                Predicate stateTopPre = criteriaBuilder.equal(root.get("state").as(String.class), Constants.Article.STATE_TOP);
                Predicate or = criteriaBuilder.or(statePublishPre, stateTopPre);
                return criteriaBuilder.and(or, labelPre);
            }
        }, pageable);
        return ResponseResult.SUCCESS("获取文章列表成功").setData(all);
    }

    @Override
    public ResponseResult listLabels(int size) {
        size = this.checkSize(size);
        Sort sort = new Sort(Sort.Direction.DESC, "count");
        Pageable pageable = PageRequest.of(0, size, sort);
        Page<Label> all = labelDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取标签成功").setData(all);
    }

}
