package cn.xuanq.blog.services;

import cn.xuanq.blog.response.ResponseResult;

public interface IWebSiteInfoService {
    ResponseResult getWebSiteTitle();

    ResponseResult putWebSiteTitle(String title);

    ResponseResult getSeoInfo();

    ResponseResult putSeoInfo(String keywords, String description);

    ResponseResult getWebSiteViewCount();

    void updateViewCount();
}
