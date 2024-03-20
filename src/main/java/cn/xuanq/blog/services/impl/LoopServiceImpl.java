package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.LoopDao;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.pojo.Looper;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.ILoopService;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.IdWorker;
import cn.xuanq.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class LoopServiceImpl extends BaseService implements ILoopService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private LoopDao loopDao;

    @Override
    public ResponseResult addLoop(Looper looper) {
        //检查数据
        String title = looper.getTitle();
        if (TextUtils.isEmpty(title)) {
            return ResponseResult.FAILED("标题不可以为空");
        }
        String imageUrl = looper.getImageUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            return ResponseResult.FAILED("图片地址不可以为空");
        }
        String targetUrl = looper.getTargetUrl();
        if (TextUtils.isEmpty(targetUrl)) {
            return ResponseResult.FAILED("跳转链接不可以为空");
        }
        //补充数据
        looper.setId(idWorker.nextId() + "");
        looper.setCreateTime(new Date());
        looper.setUpdateTime(new Date());
        //保存数据
        loopDao.save(looper);
        //返回结果
        return ResponseResult.SUCCESS("添加成功");
    }

    @Override
    public ResponseResult getLoop(String loopId) {
        Looper loop = loopDao.findOneById(loopId);
        if (loop == null) {
            return ResponseResult.FAILED("轮播图不存在");
        }
        return ResponseResult.SUCCESS("轮播图获取成功").setData(loop);
    }

    @Autowired
    private IUserService userService;

    @Override
    public ResponseResult listLoops() {
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        List<Looper> all;
        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null || !Constants.User.ROLE_ADMIN.equals(inisUser.getRoles())) {
            // 只能获取到正常的category
            all = loopDao.listLoopByState("1");
        } else {
            // 查询
            all = loopDao.findAll(sort);
        }
        return ResponseResult.SUCCESS("获取轮播图列表成功").setData(all);
    }

    @Override
    public ResponseResult updateLoop(String loopId, Looper looper) {
        // 找出来
        Looper loopFromDb = loopDao.findOneById(loopId);
        if (loopFromDb == null) {
            return ResponseResult.FAILED("轮播图不存在");
        }
        // 不可以为空的，要判空
        String title = looper.getTitle();
        if (!TextUtils.isEmpty(title)) {
            loopFromDb.setTitle(title);
        }
        String targetUrl = looper.getTargetUrl();
        if (!TextUtils.isEmpty(targetUrl)) {
            loopFromDb.setTargetUrl(targetUrl);
        }
        String imageUrl = looper.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            loopFromDb.setImageUrl(imageUrl);
        }
        if (!TextUtils.isEmpty(looper.getState())) {
            loopFromDb.setState(looper.getState());
        }
        loopFromDb.setOrder(looper.getOrder());
        loopFromDb.setUpdateTime(new Date());
        // 可以为空的直接设置
        // 保存回去
        loopDao.save(loopFromDb);
        return ResponseResult.SUCCESS("轮播图更新成功");
    }

    @Override
    public ResponseResult deleteLoop(String loopId) {
        loopDao.deleteById(loopId);
        return ResponseResult.SUCCESS("删除成功");
    }
}
