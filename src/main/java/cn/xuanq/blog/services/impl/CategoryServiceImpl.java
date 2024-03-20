package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.CategoryDao;
import cn.xuanq.blog.pojo.Category;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.ICategoryService;
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
import javax.xml.soap.Text;
import java.util.Date;
import java.util.List;


@Service
@Transactional
public class CategoryServiceImpl extends BaseService implements ICategoryService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private IUserService userService;

    @Override
    public ResponseResult addCategory(Category category) {
        // 检查数据
        // 必须的数据有：
        // 分类名称、分类pinyin、顺序、描述
        if (TextUtils.isEmpty(category.getName())) {
            return ResponseResult.FAILED("分类名称不可以为空");
        }

        if (TextUtils.isEmpty(category.getPinyin())) {
            return ResponseResult.FAILED("分类拼音不可以为空");
        }

        if (TextUtils.isEmpty(category.getDescription())) {
            return ResponseResult.FAILED("分类描述不可以为空");
        }
        // 补全数据
        category.setId(idWorker.nextId() + "");
        category.setStatus("1");
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());
        // 保存数据
        categoryDao.save(category);
        // 返回结果
        return ResponseResult.SUCCESS("添加分类成功");
    }

    @Override
    public ResponseResult getCategory(String categoryId) {
        Category category = categoryDao.findOneById(categoryId);
        if (category == null) {
            return ResponseResult.FAILED("分类不存在");
        }
        return ResponseResult.SUCCESS("获取分类成功").setData(category);
    }

    @Override
    public ResponseResult listCategories() {
        // 参数检查
        // 创建条件
        Sort sort = new Sort(Sort.Direction.DESC, "createTime", "order");
        // 判断用户角色，普通用户/未登录用户，只能获取到正常的category
        // 管理员账户可以拿到所有
        InisUser inisUser = userService.checkInisUser();
        List<Category> categories;
        if (inisUser == null || !Constants.User.ROLE_ADMIN.equals(inisUser.getRoles())) {
            // 只能获取到正常的category
            categories = categoryDao.listCategoriesByState("1");
        } else {
            // 查询
            categories = categoryDao.findAll(sort);
        }
        // 返回结果
        return ResponseResult.SUCCESS("获取分类列表成功").setData(categories);
    }

    @Override
    public ResponseResult updateCategory(String categoryId, Category category) {
        // 找出来
        Category categoryFromDb = categoryDao.findOneById(categoryId);
        if (categoryFromDb == null) {
            return ResponseResult.FAILED("分类不存在");
        }
        // 对内容判断，有些字段不能为空
        String name = category.getName();
        if (!TextUtils.isEmpty(name)) {
            categoryFromDb.setName(name);
        }
        String pinyin = category.getPinyin();
        if (!TextUtils.isEmpty(pinyin)) {
            categoryFromDb.setPinyin(pinyin);
        }
        String description = category.getDescription();
        if (!TextUtils.isEmpty(description)) {
            categoryFromDb.setDescription(description);
        }
        categoryFromDb.setStatus(category.getStatus());
        categoryFromDb.setOrder(category.getOrder());
        categoryFromDb.setUpdateTime(new Date());
        // 保存数据
        categoryDao.save(categoryFromDb);
        // 返回结果
        return ResponseResult.SUCCESS("分类更新成功");
    }

    @Override
    public ResponseResult deleteCategory(String categoryId) {
        int result = categoryDao.deleteCategoryByUpdateState(categoryId);
        if (result == 0) {
            return ResponseResult.FAILED("该分类不存在");
        }
        // Dao层新增方法 or 获取整个对象修改
        Category categoryFromDb = categoryDao.findOneById(categoryId);
        categoryFromDb.setUpdateTime(new Date());
        categoryDao.save(categoryFromDb);
        return ResponseResult.SUCCESS("删除分类成功");
    }


}
