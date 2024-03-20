package cn.xuanq.blog.controller.user;

import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.response.ResponseState;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.services.impl.UserServiceImpl;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.RedisUtil;
import cn.xuanq.blog.utils.TextUtils;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserApi {

    @Autowired
    private IUserService userService;

    /**
     * 初始化管理员账号-init-admin
     * @param inisUser
     * @return
     */
    @PostMapping("/admin_account")
    public ResponseResult initMangerAccount(@RequestBody InisUser inisUser, HttpServletRequest request) {

        return userService.initMangerAccount(inisUser, request);
    }

    /**
     * 注册
     * @param inisUser
     * @return
     */
    @PostMapping("/join_in")
    public ResponseResult register(@RequestBody InisUser inisUser,
                                   @RequestParam("email_code") String emailCode,
                                   @RequestParam("captcha_code") String captchaCode,
                                   @RequestParam("captcha_key") String captchaKey,
                                   HttpServletRequest request) {
        // 第一步：检查当前用户名是否已经注册
        // 第二步：检查邮箱格式是否正确
        // 第三步：检查该邮箱是否已经注册
        // 第四步：检查邮箱验证码是否正确
        // 第五步：检查图灵验证码是否正确
        // 达到可以注册的条件
        // 第六步：对密码进行加密
        // 第七步：补全数据
        // 包括：注册IP,登录IP,角色,头像,创建时间,更新时间
        // 第八步：保存到数据库中
        // 第九步：返回结果
        // 邮箱校验
        return userService.register(inisUser, emailCode, captchaCode, captchaKey ,request);
    }

    /**
     * 登录 sign-up
     * @param captcha
     * @param inisUser
     * @param captchaKey
     * @return
     */
    @PostMapping("/login/{captcha}/{captcha_key}")
    public ResponseResult login(@PathVariable("captcha_key") String captchaKey,
                                @PathVariable("captcha") String captcha,
                                @RequestBody InisUser inisUser,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        return userService.doLogin(captcha, captchaKey, inisUser, request, response);
    }


    /**
     * 获取图灵验证码
     * @return
     */
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response, @RequestParam("captcha_key") String captchaKey) throws Exception {
        try{
            userService.createCaptcha(response, captchaKey);
        }catch (Exception e) {
            log.error(e.toString());
        }

    }

    /**
     * 发送邮件email
     * 使用场聚:注册、找回密码、修改邮箱(会输入新的邮箱)
     * 注册：如果已经法册过了，就提示说 该邮箱已经注册
     * 找回密码:如果没有注册过,提示该邮箱没有注册
     * 修改邮箱(新的邮箱):如果已经法册了、提示该邮箱已经注册
     * @return
     */
    @GetMapping("/verify_code")
    public ResponseResult sendVerifyCode(HttpServletRequest request, @RequestParam("type") String type, @RequestParam("email") String emailAddress) {
        log.info("email ==> " + emailAddress);
        return userService.sendEmail(type, request, emailAddress);
    }

    /**
     * 修改密码
     * 修改密码、找回密码
     * 普通做法：通过旧密码对比来更新密码
     *
     * 既可以找回密码，也可以修改密码
     * 发送验证码 --> 判断验证码是否正确
     * 对应邮箱所注册的账号是否属于你
     * @param inisUser
     * @return
     */
    @PutMapping("/password/{verifyCode}")
    public ResponseResult updatePassword(@PathVariable("verifyCode") String verifyCode, @RequestBody InisUser inisUser){
        return userService.updateUserPassword(verifyCode, inisUser);
    }

    /**
     * 获取作者信息user-info
     * @return
     */
    @GetMapping("/user_info/{userId}")
    public ResponseResult getUserInfo(@PathVariable("userId") String userId) {
        return userService.getUserInfo(userId);
    }

    /**
     * 修改用户信息user-info
     * @param userId
     * @param inisUser
     * @return
     */
    @PutMapping("/user_info/{userId}")
    public ResponseResult updateUserInfo(@PathVariable("userId") String userId, @RequestBody InisUser inisUser) {
        return userService.updateUserInfo(userId, inisUser);
    }

    /**
     * 获取用户列表
     * 权限 管理员权限
     * @param page
     * @param size
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @GetMapping("/list")
    public ResponseResult listUsers(@RequestParam("page") int page,
                                    @RequestParam("size") int size,
                                    @RequestParam(value = "userName", required = false) String userName,
                                    @RequestParam(value = "email", required = false) String email) {
        return userService.listUsers(page, size, userName, email);
    }

    /**
     * 需要管理员权限
     *
     * @param userId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{userId}")
    public ResponseResult deleteUser(@PathVariable("userId") String userId) {
        // 判断当前操作是谁
        // 根据用户判断是否可以删除
        // TODO:通过注解的方式控制权限
        return userService.deleteUserById(userId);
    }

    /**
     * 检查email是否已经注册
     * @param email 邮箱地址
     * @return  SUCCESS    FAILED
     */
    @ApiResponses({
            @ApiResponse(code = 20000, message = "表示当前邮箱已经注册了"),
            @ApiResponse(code = 40000, message = "表示当前邮箱未注册")
    })
    @GetMapping("/email")
    public ResponseResult checkEmail(@RequestParam("email") String email) {
        return userService.checkEmail(email);
    }

    /**
     * 检查用户名是否已经注册
     * @param userName 用户名
     * @return  SUCCESS    FAILED
     */
    @ApiResponses({
            @ApiResponse(code = 20000, message = "表示当前用户名已经注册了"),
            @ApiResponse(code = 40000, message = "表示当前用户名未注册")
    })
    @GetMapping("/user_name")
    public ResponseResult checkUserName(@RequestParam("userName") String userName) {
        return userService.checkUserName(userName);
    }


    /**
     * 更新邮箱
     * 1.已经登录
     * 2.新的邮箱没有注册过
     *
     * 用户的步骤;
     * 1、已经登录
     * 2、输入新的邮箱地址
     * 3、获取验证码type=update
     * 4、输入验证码
     * 5、提交数据
     * @return
     */
    @PutMapping("/email")
    public ResponseResult updateEmail(@RequestParam("email") String email,
                                      @RequestParam("verify_code") String verifyCode) {
        return userService.updateEmail(email, verifyCode);
    }


    /**
     *退出登录
     * 拿到token_key
     * 删除redis对应的token
     * 删除mysql里对应的refreshToken
     * 删除cookie里的token_key
     * @return
     */
    @GetMapping("/logout")
    public ResponseResult logout() {
        return userService.doLogout();
    }

    @GetMapping("/check-token")
    public ResponseResult parseToken() {
        return userService.parseToken();
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/reset-password/{userId}")
    public ResponseResult resetPassword(@PathVariable("userId") String userId, @RequestParam("password") String password) {
        return userService.resetPassword(userId, password);
    }

}
