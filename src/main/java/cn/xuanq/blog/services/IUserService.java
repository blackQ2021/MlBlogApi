package cn.xuanq.blog.services;

import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.response.ResponseResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IUserService {
    ResponseResult initMangerAccount(InisUser inisUser, HttpServletRequest request);


    void createCaptcha(HttpServletResponse response, String captchaKey) throws Exception;

    ResponseResult sendEmail(String type, HttpServletRequest request, String emailAddress);

    ResponseResult register(InisUser inisUser, String emailCode, String captchaCode, String captchaKey, HttpServletRequest request);

    ResponseResult doLogin(String captcha, String captchaKey, InisUser inisUser, HttpServletRequest request, HttpServletResponse response);

    InisUser checkInisUser();

    ResponseResult getUserInfo(String userId);

    ResponseResult checkEmail(String email);

    ResponseResult checkUserName(String userName);

    ResponseResult updateUserInfo(String userId, InisUser inisUser);

    ResponseResult deleteUserById(String userId);

    ResponseResult updateUserPassword(String verifyCode, InisUser inisUser);

    ResponseResult updateEmail(String email, String verifyCode);

    ResponseResult doLogout();

    ResponseResult parseToken();

    ResponseResult listUsers(int page, int size, String userName, String email);

    ResponseResult resetPassword(String userId, String password);
}

