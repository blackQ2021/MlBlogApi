package cn.xuanq.blog.services;

import cn.xuanq.blog.pojo.Category;
import cn.xuanq.blog.response.ResponseResult;

public interface ICategoryService {
    ResponseResult addCategory(Category category);

    ResponseResult getCategory(String categoryId);

    ResponseResult listCategories();

    ResponseResult updateCategory(String categoryId, Category category);

    ResponseResult deleteCategory(String categoryId);
}
