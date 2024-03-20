package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.CookieUtils;
import cn.xuanq.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service("permission")
public class PermissionService {

    @Autowired
    private IUserService userService;

    /**
     * 判断是不是管理员
     * @return
     */
    public boolean admin() {
        // 获取到当前权限所有的角色，进行角色对比即可确定权限
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        //如果token返回false
        String tokenKey = CookieUtils.getCookie(request, Constants.User.COOKIE_TOKEN_KEY);
        if (TextUtils.isEmpty(tokenKey)) {
            return false;
        }

        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null || TextUtils.isEmpty(inisUser.getRoles())) {
            return false;
        }
        if (Constants.User.ROLE_ADMIN.equals(inisUser.getRoles())) {
            return true;
        }
        return false;
    }

}
