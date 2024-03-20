package cn.xuanq.blog.dao;

import cn.xuanq.blog.pojo.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentDao extends JpaRepository<Comment,String>, JpaSpecificationExecutor<Comment> {

    Comment findOneById(String commentId);

    @Modifying
    @Query(nativeQuery = true, value = "update `tb_comment` set `state` = '0' where `article_id` = ?")
    void deleteAllByArticleId(String articleId);

    Page<Comment> findByArticleId(String articleId, Pageable pageable);
}
