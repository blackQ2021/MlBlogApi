package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.FriendLinkDao;
import cn.xuanq.blog.pojo.FriendLink;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.IFriendLinkService;
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
public class FriendLinkServiceImpl extends BaseService implements IFriendLinkService {


    @Autowired
    private IdWorker idWorker;

    @Autowired
    private FriendLinkDao friendLinkDao;

    /**
     * 添加友情链接
     * @param friendLink
     * @return
     */
    @Override
    public ResponseResult addFriendLink(FriendLink friendLink) {
        // 判断数据
        String url = friendLink.getUrl();
        if (TextUtils.isEmpty(url)) {
            return ResponseResult.FAILED("链接url不可以为空");
        }
        String logo = friendLink.getLogo();
        if (TextUtils.isEmpty(logo)) {
            return ResponseResult.FAILED("logo不可以为空");
        }
        String name = friendLink.getName();
        if (TextUtils.isEmpty(name)) {
            return ResponseResult.FAILED("网站名不可以为空");
        }
        // 补全数据
        friendLink.setId(idWorker.nextId() + "");
        friendLink.setCreateTime(new Date());
        friendLink.setUpdateTime(new Date());
        // 保存数据
        friendLinkDao.save(friendLink);
        // 返回结果
        return ResponseResult.SUCCESS("添加成功");
    }

    @Override
    public ResponseResult getFriendLink(String friendLinkId) {
        FriendLink friendLink = friendLinkDao.findOneById(friendLinkId);
        if (friendLink == null) {
            return ResponseResult.FAILED("该友情链接不存在");
        }
        return ResponseResult.SUCCESS("获取成功");
    }

    @Autowired
    private IUserService userService;

    @Override
    public ResponseResult listFriendLinks() {
        // 创建条件
        Sort sort = new Sort(Sort.Direction.DESC, "createTime", "order");
        List<FriendLink> all;
        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null || !Constants.User.ROLE_ADMIN.equals(inisUser.getRoles())) {
            // 只能获取到正常的category
            all = friendLinkDao.listFriendLinkByState("1");
        } else {
            // 查询
            all = friendLinkDao.findAll(sort);
        }
        return ResponseResult.SUCCESS("获取友情链接列表成功").setData(all);
    }

    @Override
    public ResponseResult deleteFriendLink(String friendLinkId) {
        int result = friendLinkDao.deleteAllById(friendLinkId);
        if (result == 0) {
            return ResponseResult.FAILED("删除失败");
        }
        return ResponseResult.SUCCESS("删除成功");
    }

    /**
     * 更新内容有什么
     * logo
     * 网站名称
     * URL
     * order
     * 
     * @param friendLinkId
     * @param friendLink
     * @return
     */
    @Override
    public ResponseResult updateFriendLink(String friendLinkId, FriendLink friendLink) {
        FriendLink friendLinkFromDb = friendLinkDao.findOneById(friendLinkId);
        if (friendLinkFromDb == null) {
            return ResponseResult.FAILED("更新失败");
        }
        String logo = friendLink.getLogo();
        if (!TextUtils.isEmpty(logo)) {
            friendLinkFromDb.setLogo(logo);
        }
        String name = friendLink.getName();
        if (!TextUtils.isEmpty(name)) {
            friendLinkFromDb.setName(name);
        }
        String url = friendLink.getUrl();
        if (!TextUtils.isEmpty(url)) {
            friendLinkFromDb.setUrl(url);
        }
        String state = friendLink.getState();
        if (!TextUtils.isEmpty(state)) {
            friendLinkFromDb.setState(state);
        }
        friendLinkFromDb.setOrder(friendLink.getOrder());
        friendLinkFromDb.setUpdateTime(new Date());
        // 保存数据
        friendLinkDao.save(friendLinkFromDb);
        return ResponseResult.SUCCESS("更新成功");
    }


}
