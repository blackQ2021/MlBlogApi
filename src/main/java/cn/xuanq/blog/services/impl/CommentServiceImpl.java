package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.ArticleNoContentDao;
import cn.xuanq.blog.dao.CommentDao;
import cn.xuanq.blog.pojo.ArticleNoContent;
import cn.xuanq.blog.pojo.Comment;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.pojo.PageList;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.ICommentService;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.IdWorker;
import cn.xuanq.blog.utils.RedisUtil;
import cn.xuanq.blog.utils.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;

@Service
@Transactional
public class CommentServiceImpl extends BaseService implements ICommentService {

    @Autowired
    private IUserService userService;

    @Autowired
    private ArticleNoContentDao articleNoContentDao;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CommentDao commentDao;

    /**
     * 发表评论
     * @param comment
     * @return
     */
    @Override
    public ResponseResult postComment(Comment comment) {
        // 检查用户是否登录
        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        // 检查内容
        String articleId = comment.getArticleId();
        if (TextUtils.isEmpty(articleId)) {
            return ResponseResult.FAILED("文章ID不可以为空");
        }
        ArticleNoContent article = articleNoContentDao.findOneById(articleId);
        if (article == null) {
            return ResponseResult.FAILED("文章不存在");
        }
        String content = comment.getContent();
        if (TextUtils.isEmpty(content)) {
            return ResponseResult.FAILED("评论内容不可以为空");
        }
        // 补全内容
        comment.setId(idWorker.nextId() + "");
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());
        comment.setUserAvatar(inisUser.getAvatar());
        comment.setUserName(inisUser.getUserName());
        comment.setUserId(inisUser.getId());
        // 保存入库
        commentDao.save(comment);
//        清除评论缓存
        redisUtil.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + comment.getArticleId());
        // 返回结果
        return ResponseResult.SUCCESS("评论成功");
    }

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private Gson gson;
    /**
     * 获取文章评论
     * 评论的排序策略:
     * 最基本的就按时间排序-->升序和降序-->先发表的在前面或者后发表的在前面
     *
     * 置顶的，一定在最前面
     * 后发表的:前单位时间内会排在前面，过了此单位时间，会按点赞量和发表时间进行排序
     * @param articleId
     * @param page
     * @param size
     * @return
     */
    @Override
    public ResponseResult listCommentsByArticleId(String articleId, int page, int size) {
        page = checkPage(page);
        size = checkSize(size);
        //        如果是第一页，先从缓存中拿
        String cacheJson = (String) redisUtil.get(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId);
        if (!TextUtils.isEmpty(cacheJson) && page == 1) {
            PageList<Comment> result = gson.fromJson(cacheJson, new TypeToken<PageList<Comment>>() {
            }.getType());
            return ResponseResult.SUCCESS("评论列表获取成功").setData(result);
        }
//        如果有就返回
//        如果没有就往下走
        Sort sort = new Sort(Sort.Direction.DESC, "state", "createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Comment> all = commentDao.findByArticleId(articleId, pageable);
//        把结果转成pageList
        PageList<Comment> result = new PageList<>();
        result.parsePage(all);
//        保存一份到缓存
        if (page == 1) {
            redisUtil.set(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId, gson.toJson(result), Constants.TimeValueInSecond.MIN_15);
        }
        return ResponseResult.SUCCESS("评论列表获取成功").setData(result);
    }

    @Override
    public ResponseResult deleteCommentById(String commentId) {
        // 检查用户角色
        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        // 把评论找出来，对比用户权限
        Comment comment = commentDao.findOneById(commentId);
        if (comment == null) {
            return ResponseResult.FAILED("评论不存在");
        }
        // 用户ID不一样，只有管理员才能删
        if (inisUser.getId().equals(comment.getUserId()) || Constants.User.ROLE_ADMIN.equals(inisUser.getRoles())) {
            // 登录判断角色
            // 用户Id一致，说明此评论是当前用户
            commentDao.deleteById(commentId);
            return ResponseResult.SUCCESS("评论删除成功");
        } else {
            return ResponseResult.PERMISSION_DENIED()  ;
        }
    }

    @Override
    public ResponseResult listComments(int page, int size) {
        page = checkPage(page);
        size = checkSize(size);
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page -1, size, sort);
        Page<Comment> all = commentDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取评论列表成功").setData(all);
    }

    @Override
    public ResponseResult topComment(String commentId) {
        Comment comment = commentDao.findOneById(commentId);
        if (comment == null) {
            return ResponseResult.FAILED("评论不存在");
        }
        String state = comment.getState();
        if (Constants.Comment.STATE_PUBLISH.equals(state)) {
            comment.setState(Constants.Comment.STATE_TOP);
            return ResponseResult.SUCCESS("置顶成功");
        } else if (Constants.Comment.STATE_TOP.equals(state)) {
            comment.setState(Constants.Comment.STATE_PUBLISH);
            return ResponseResult.SUCCESS("取消置顶");
        } else {
            return ResponseResult.FAILED("评论状态非法");
        }
    }
}
