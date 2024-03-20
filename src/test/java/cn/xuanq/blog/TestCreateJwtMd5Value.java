package cn.xuanq.blog;

import org.springframework.util.DigestUtils;

public class TestCreateJwtMd5Value {
    public static void main(String[] args) {
//        171c5666ae9077a2c6dd629a11ad9818
        String jwtKeyMd5Str = DigestUtils.md5DigestAsHex("inis_blog_system_-=".getBytes());
        System.out.println(jwtKeyMd5Str);
    }
}
