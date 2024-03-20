package cn.xuanq.blog.utils;

import cn.xuanq.blog.pojo.InisUser;
import io.jsonwebtoken.Claims;

import java.util.HashMap;
import java.util.Map;

public class ClaimsUtils {

    public static final String ID = "id";
    public static final String USERNAME = "userName";
    public static final String ROLES = "roles";
    public static final String AVATAR = "avatar";
    public static final String EMAIL = "email";
    public static final String SIGN = "sign";

    public static Map<String, Object> inisUser2Claims(InisUser inisUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ID, inisUser.getId());
        claims.put(USERNAME, inisUser.getUserName());
        claims.put(ROLES, inisUser.getRoles());
        claims.put(AVATAR, inisUser.getAvatar());
        claims.put(EMAIL, inisUser.getEmail());
        claims.put(SIGN, inisUser .getSign());
        return claims;
    }

    public static InisUser claims2InisUser(Claims claims) {
        InisUser inisUser = new InisUser();
        String id = (String) claims.get(ID);
        inisUser.setId(id);
        String userName = (String) claims.get(USERNAME);
        inisUser.setUserName(userName);
        String roles = (String) claims.get(ROLES);
        inisUser.setRoles(roles);
        String avatar = (String) claims.get(AVATAR);
        inisUser.setAvatar(avatar);
        String email = (String) claims.get(EMAIL);
        inisUser.setEmail(email);
        String sign = (String) claims.get(SIGN);
        inisUser.setSign(sign);
        return inisUser;
    }
}
