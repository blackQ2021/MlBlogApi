package cn.xuanq.blog.dao;

import cn.xuanq.blog.pojo.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RefreshTokenDao extends JpaRepository<RefreshToken, String>, JpaSpecificationExecutor<RefreshToken> {
    RefreshToken findOneByTokenKey(String tokenKey);

    int deleteAllByUserId(String userId);

    int deleteAllByTokenKey(String tokenKey);
}
