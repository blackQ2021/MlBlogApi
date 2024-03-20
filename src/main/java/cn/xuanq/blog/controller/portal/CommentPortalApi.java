package cn.xuanq.blog.controller.portal;

import cn.xuanq.blog.pojo.Comment;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.ICommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portal/comment")
public class CommentPortalApi {

    @Autowired
    private ICommentService commentService;

    @PostMapping
    public ResponseResult postComment(@RequestBody Comment comment) {
        return commentService.postComment(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseResult deleteComment(@PathVariable("commentId") String commentId) {
        return commentService.deleteCommentById(commentId);
    }

    @GetMapping("/list/{articleId}/{page}/{size}")
    public ResponseResult listComments(@PathVariable("articleId") String articleId,
                                       @PathVariable("page") int page,
                                       @PathVariable("size") int size) {
        return commentService.listCommentsByArticleId(articleId, page, size);
    }

}
