package cn.xuanq.blog.controller.portal;


import cn.xuanq.blog.dao.CommentDao;
import cn.xuanq.blog.dao.LabelDao;
import cn.xuanq.blog.pojo.Comment;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.pojo.Label;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.utils.*;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@Transactional
@RequestMapping("/portal/test")
public class TestController {

    @Autowired
    private IdWorker idWorker;
    @Autowired
    private LabelDao labelDao;

    @PostMapping("/label")
    public ResponseResult addLabel(@RequestBody Label label) {
        //判断数据是否有效
        //补全数据
        label.setId(idWorker.nextId() + "");
        label.setCreateTime(new Date());
        label.setUpdateTime(new Date());
        //保存数据
        labelDao.save(label);
        return ResponseResult.SUCCESS("测试标签添加成功");
    }

    @DeleteMapping("/label/{labelId}")
    public ResponseResult deleteLabel(@PathVariable("labelId") String labelId) {
//        int deleteResult = labelDao.deleteOneById(labelId);
        // 自定义customDeleteLabelById ==> sql语句
        int deleteResult = labelDao.customDeleteLabelById(labelId);
        if (deleteResult > 0) {
            return ResponseResult.SUCCESS("删除标签成功");
        }else {
            return ResponseResult.FAILED("标签不存在");
        }
    }

    @PutMapping("/label/{labelId}")
    public ResponseResult updateLabel(@PathVariable("labelId") String labelId, @RequestBody Label label) {
        Label dbLabel = labelDao.findOneById(labelId);
        if (dbLabel == null) {
            return ResponseResult.FAILED("标签不存在");
        }
        dbLabel.setCount(label.getCount());
        dbLabel.setName(label.getName());
        dbLabel.setUpdateTime(new Date());
        labelDao.save(dbLabel);
        return ResponseResult.SUCCESS("修改成功");
    }

    @GetMapping("/label/{labelId}")
    public ResponseResult getLabelById(@PathVariable("labelId") String labelId) {
        Label dbLabel = labelDao.findOneById(labelId);
        if (dbLabel == null) {
            return ResponseResult.FAILED("标签不存在");
        }
        return ResponseResult.SUCCESS("获取标签成功").setData(dbLabel);
    }

    @GetMapping("/list/label/{page}/{size}")
    public ResponseResult listLabels(@PathVariable("page") int page, @PathVariable("size") int size) {
        if (page < 1) {
            page = 1;
        }
        if (size <= 0) {
            size = Constants.Page.DEFAULT_SIZE;
        }
        Sort sort = new Sort(Sort.Direction.DESC,"createTime");
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Label> result = labelDao.findAll(pageable);
        return ResponseResult.SUCCESS("获取成功").setData(result);
    }

    @GetMapping("/label/search")
    public ResponseResult doLabelSearch(@RequestParam("keyword") String keyword, @RequestParam("count") int count) {
        List<Label> all = labelDao.findAll(new Specification<Label>() {
            @Override
            public Predicate toPredicate(Root<Label> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                // equal  like 条件查询
                Predicate namePre = cb.like(root.get("name").as(String.class), "%" + keyword + "%");
                Predicate countPre = cb.equal(root.get("count").as(Integer.class), count);
                Predicate and = cb.and(namePre, countPre);
                return and;
            }
        });
        if (all.size() == 0) {
            return ResponseResult.FAILED("结果为空");
        }
        return ResponseResult.SUCCESS("查找成功").setData(all);
    }

    @Autowired
    private RedisUtil redisUtil;

    //http://localhost:2022/test/captcha
    @RequestMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 设置请求头为输出图片类型
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 三个参数分别为宽、高、位数
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        // 设置字体
        // specCaptcha.setFont(new Font("Verdana", Font.PLAIN, 32));  // 有默认字体，可以不用设置
        specCaptcha.setFont(Captcha.FONT_1);
        // 设置类型，纯数字、纯字母、字母数字混合
        //specCaptcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        specCaptcha.setCharType(Captcha.TYPE_DEFAULT);

        String content = specCaptcha.text().toLowerCase();
        // 验证码存入session 与用户提交的验证码比对
        request.getSession().setAttribute("captcha", content);
        log.info("content ==> " + content);

        // 前后端分离
        // 保存到 redis 里，10分钟有效
        // redisUtil.set(Constants.User.KEY_CAPTCHA_CONTENT + "123456", content, 60 * 10);

        // 输出图片流
        specCaptcha.out(response.getOutputStream());
    }

    @Autowired
    private CommentDao commentDao;

    @PostMapping("/comment")
    public ResponseResult testComment(@RequestBody Comment comment, HttpServletRequest request) {
        String content = comment.getContent();
        log.info("comment == >" + content);
        String tokenKey = CookieUtils.getCookie(request, Constants.User.COOKIE_TOKEN_KEY);
        if (tokenKey == null) {
            return ResponseResult.FAILED("账号未登录");
        }

        String token = (String) redisUtil.get(Constants.User.KEY_TOKEN + tokenKey);
        if (token == null) {

        }
        Claims claims = null;
        try {
            claims = JwtUtil.parseJWT(token);
        }catch (Exception e){
            // 过期了，查refreshToken
            return ResponseResult.FAILED("账号未登录");
        }
        if (claims == null) {
            return ResponseResult.FAILED("账号未登录");
        }
        // 已经登陆了
        InisUser inisUser = ClaimsUtils.claims2InisUser(claims);
        comment.setUserId(inisUser.getId());
        comment.setUserAvatar(inisUser.getAvatar());
        comment.setUserName(inisUser.getUserName());
        comment.setCreateTime(new Date());
        comment.setUpdateTime(new Date());
        comment.setId(idWorker.nextId() + "");
        commentDao.save(comment);
        return ResponseResult.SUCCESS("评论成功");
    }

}






























