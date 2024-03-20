package cn.xuanq.blog.services;

import cn.xuanq.blog.pojo.Comment;
import cn.xuanq.blog.response.ResponseResult;

public interface ICommentService {
    ResponseResult postComment(Comment comment);

    ResponseResult listCommentsByArticleId(String articleId, int page, int size);

    ResponseResult deleteCommentById(String commentId);

    ResponseResult listComments(int page, int size);

    ResponseResult topComment(String commentId);
}
