package cn.xuanq.blog.services;

import cn.xuanq.blog.pojo.Article;
import cn.xuanq.blog.response.ResponseResult;

public interface IArticleService {
    ResponseResult postArticle(Article article);

    ResponseResult listArticles(int page, int size, String keyword, String categoryId, String state);

    ResponseResult getArticleById(String articleId);

    ResponseResult updateArticle(String articleId, Article article);

    ResponseResult deleteArticleById(String articleId);

    ResponseResult deleteArticleByUpdateState(String articleId);

    ResponseResult topArticle(String articleId);

    ResponseResult listTopArticles();

    ResponseResult listRecommendArticle(String articleId, int size);

    ResponseResult listArticleByLabel(int page, int size, String label);

    ResponseResult listLabels(int size);
}
