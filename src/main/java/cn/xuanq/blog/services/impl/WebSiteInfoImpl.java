package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.SettingDao;
import cn.xuanq.blog.pojo.Setting;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.IWebSiteInfoService;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.IdWorker;
import cn.xuanq.blog.utils.RedisUtil;
import cn.xuanq.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class WebSiteInfoImpl extends BaseService implements IWebSiteInfoService {

    @Autowired
    private SettingDao settingDao;

    @Autowired
    private IdWorker idWorker;

    @Override
    public ResponseResult getWebSiteTitle() {
        Setting title = settingDao.findOneByKey(Constants.Settings.WEB_SITE_TITLE);
        return ResponseResult.SUCCESS("获取网站title成功").setData(title);
    }

    @Override
    public ResponseResult putWebSiteTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            return ResponseResult.FAILED("网站标题不可以为空");
        }
        Setting titleFromDb = settingDao.findOneByKey(Constants.Settings.WEB_SITE_TITLE);
        if (titleFromDb == null) {
            titleFromDb = new Setting();
            titleFromDb.setId(idWorker.nextId() + "");
            titleFromDb.setCreateTime(new Date());
            titleFromDb.setUpdateTime(new Date());
            titleFromDb.setKey(Constants.Settings.WEB_SITE_TITLE);
        }
        titleFromDb.setValue(title);
        settingDao.save(titleFromDb);
        return ResponseResult.SUCCESS("网站title更新成功");
    }

    @Override
    public ResponseResult getSeoInfo() {
        Setting description = settingDao.findOneByKey(Constants.Settings.WEB_SITE_DESCRIPTION);
        Setting keyWords = settingDao.findOneByKey(Constants.Settings.WEB_SITE_KEYWORDS);
        Map<String, String> result = new HashMap<>();
        result.put(description.getKey(), description.getValue());
        result.put(keyWords.getKey(), keyWords.getValue());
        return ResponseResult.SUCCESS("获取SEO信息成功").setData(result);
    }

    @Override
    public ResponseResult putSeoInfo(String keywords, String description) {
        // 判断
        if (TextUtils.isEmpty(keywords)) {
            return ResponseResult.FAILED("关键字不可以为空");
        }
        if (TextUtils.isEmpty(description)) {
            return ResponseResult.FAILED("描述不可以为空");
        }
        Setting descriptionFromDb = settingDao.findOneByKey(Constants.Settings.WEB_SITE_DESCRIPTION);
        if (descriptionFromDb == null) {
            descriptionFromDb = new Setting();
            descriptionFromDb.setId(idWorker.nextId() + "");
            descriptionFromDb.setCreateTime(new Date());
            descriptionFromDb.setUpdateTime(new Date());
            descriptionFromDb.setKey(Constants.Settings.WEB_SITE_DESCRIPTION);
        }
        descriptionFromDb.setValue(description);
        settingDao.save(descriptionFromDb);

        Setting keyWordsFromDb = settingDao.findOneByKey(Constants.Settings.WEB_SITE_KEYWORDS);
        if (keyWordsFromDb == null) {
            keyWordsFromDb = new Setting();
            keyWordsFromDb.setId(idWorker.nextId() + "");
            keyWordsFromDb.setCreateTime(new Date());
            keyWordsFromDb.setUpdateTime(new Date());
            keyWordsFromDb.setKey(Constants.Settings.WEB_SITE_KEYWORDS);
        }
        keyWordsFromDb.setValue(keywords);
        settingDao.save(keyWordsFromDb);
        return ResponseResult.SUCCESS("更新SEO信息成功");
    }

    /**
     * 这个是全网站的访问量，要做的细一点，还得分来源
     * 这里只统计浏览量，只统计文章的浏览量，提供一个浏览量的统计接口（页面级的）
     * @return 浏览量
     */
    @Override
    public ResponseResult getWebSiteViewCount() {
        // 先从redis里拿出来
        String viewCountStr = (String) redisUtil.get(Constants.Settings.WEB_SITE_VIEW_COUNT);
        Setting viewCount = settingDao.findOneByKey(Constants.Settings.WEB_SITE_VIEW_COUNT);
        if (viewCount == null) {
            viewCount = this.initViewCount();
            settingDao.save(viewCount);
        }
        if (TextUtils.isEmpty(viewCountStr)) {
            viewCountStr = viewCount.getValue();
            redisUtil.set(Constants.Settings.WEB_SITE_VIEW_COUNT, viewCountStr);
        } else {
            // 把redis里的更新到数据库里
            viewCount.setValue(viewCountStr);
            settingDao.save(viewCount);
        }
        Map<String, Integer> result = new HashMap<>();
        result.put(viewCount.getKey(), Integer.valueOf(viewCount.getValue()));
        return ResponseResult.SUCCESS("获取网站浏览量成功").setData(result);
    }

    private Setting initViewCount() {
        Setting viewCount = new Setting();
        viewCount.setId(idWorker.nextId() + "");
        viewCount.setKey(Constants.Settings.WEB_SITE_VIEW_COUNT);
        viewCount.setUpdateTime(new Date());
        viewCount.setUpdateTime(new Date());
        viewCount.setValue("1");
        return viewCount;
    }

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 1.并发量
     * 2.过滤相同IP/ID
     * 3.防止攻击，比如太频繁的访问，就提示稍后重试
     */
    @Override
    public void updateViewCount() {
        // redis更新时机
        Object viewCount = redisUtil.get(Constants.Settings.WEB_SITE_VIEW_COUNT);
        if (viewCount == null) {
            Setting setting = settingDao.findOneByKey(Constants.Settings.WEB_SITE_VIEW_COUNT);
            if (setting == null) {
                setting = this.initViewCount();
                settingDao.save(setting);
            }
            redisUtil.set(Constants.Settings.WEB_SITE_VIEW_COUNT, setting.getValue());
        } else {
            // 自增
            redisUtil.incr(Constants.Settings.WEB_SITE_VIEW_COUNT, 1);
        }
    }
}
