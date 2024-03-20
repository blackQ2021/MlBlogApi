package cn.xuanq.blog.controller.admin;

import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.IWebSiteInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/web_site_info")
public class WebSiteInfoAdminApi {

    @Autowired
    private IWebSiteInfoService webSiteInfoService;

    @PreAuthorize("@permission.admin()")
    @GetMapping("/title")
    public ResponseResult getWebSiteTitle(){
        return webSiteInfoService.getWebSiteTitle();
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/title")
    public ResponseResult upWebSiteTitle(@RequestParam("title") String title){
        return webSiteInfoService.putWebSiteTitle(title);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/seo")
    public ResponseResult getSeoInfo(){
        return webSiteInfoService.getSeoInfo();
    }

    @PreAuthorize("@permission.admin()")
    @PutMapping("/seo")
    public ResponseResult putSeoInfo(@RequestParam("keywords") String keywords, @RequestParam("description") String description){
        return webSiteInfoService.putSeoInfo(keywords, description);
    }

    @PreAuthorize("@permission.admin()")
    @GetMapping("/view_count")
    public ResponseResult getWebSiteViewCount(){
        return webSiteInfoService.getWebSiteViewCount();
    }

}
