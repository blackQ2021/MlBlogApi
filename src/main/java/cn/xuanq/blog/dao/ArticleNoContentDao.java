package cn.xuanq.blog.dao;

import cn.xuanq.blog.pojo.ArticleNoContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleNoContentDao extends JpaRepository<ArticleNoContent, String>, JpaSpecificationExecutor<ArticleNoContent> {

    ArticleNoContent findOneById(String id);

    @Query(nativeQuery = true, value = "select * from `tb_article` where `labels` like ? and `id` != ? and (`state` = '1' or `state` = '3') limit ?")
    List<ArticleNoContent> listArticleByLikeLabel(String label, String originalArticleId, int size);

    @Query(nativeQuery = true, value = "select * from `tb_article` where `id` != ? and (`state` = '1' or `state` = '3') order by `create_time` desc limit ?")
    List<ArticleNoContent> listLastArticleBySize(String originalArticleId, int size);
}
