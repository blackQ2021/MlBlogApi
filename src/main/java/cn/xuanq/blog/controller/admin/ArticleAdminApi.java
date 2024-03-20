package cn.xuanq.blog.controller.admin;

import cn.xuanq.blog.pojo.Article;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.IArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/article")
public class ArticleAdminApi {

    @Autowired
    private IArticleService articleService;

    @PreAuthorize("@permission.admin()")
    @PostMapping
    public ResponseResult postArticle(@RequestBody Article article) {
        return articleService.postArticle(article);
    }

    /**
     * 如果是多用户，用户不可以别除，删除只是修改状态
     * 管理可以删除
     *
     * 做成真的删除
     * @param articleId
     * @return
     */
    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/{articleId}")
    public ResponseResult deleteArticle(@PathVariable("articleId") String articleId) {
        return articleService.deleteArticleById(articleId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/{articleId}")
    public ResponseResult updateArticle(@PathVariable("articleId") String articleId,
                                        @RequestBody Article article) {
        return articleService.updateArticle(articleId, article);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/{articleId}")
    public ResponseResult getArticle(@PathVariable("articleId") String articleId) {
        return articleService.getArticleById(articleId);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/list/{page}/{size}")
    public ResponseResult listArticles(@PathVariable("page") int page,
                                       @PathVariable("size") int size,
                                       @RequestParam(value = "state", required = false) String state,
                                       @RequestParam(value = "keyword", required = false) String keyword,
                                       @RequestParam(value = "categoryId", required = false) String categoryId) {
        return articleService.listArticles(page, size, keyword, categoryId, state);
    }

    @PreAuthorize("@permission.admin()")
    @DeleteMapping("/state/{articleId}")
    public ResponseResult deleteArticleByUpdateState(@PathVariable("articleId") String articleId) {
        return articleService.deleteArticleByUpdateState(articleId);
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/top/{articleId}")
    public ResponseResult topArticle(@PathVariable("articleId") String articleId) {
        return articleService.topArticle(articleId);
    }
}
