package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.ImageDao;
import cn.xuanq.blog.pojo.Image;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.services.IImageService;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.utils.Constants;
import cn.xuanq.blog.utils.IdWorker;
import cn.xuanq.blog.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class ImageServiceImpl extends BaseService implements IImageService {

    @Value("${inis.blog.image.save-path}")
    public String imagePath;

    @Value("${inis.blog.image.max-size}")
    public long maxSize;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private IUserService userService;

    @Autowired
    private ImageDao imageDao;

    /**
     * 上传的路径：可以配置，在配置文件里配置
     * 上传的内容，命名-->可以用id，-->每天一个文件夹
     * 限制文件大小
     * 保存记录到数据库里
     * ID / 存储路径 / url / 原名称 / 用户ID / 状态 / 创建日期 / 更新日期
     * @param file
     * @param original
     * @return
     */
    @Override
    public ResponseResult uploadImage(MultipartFile file, String original) {
        // 判断是否有文件
        if (file == null) {
            return ResponseResult.FAILED("图片不可以为空");
        }

        // 判断文件类型，我们只支持图片上传，比如说，png、jpg、gif
        String contentType = file.getContentType();
        log.info("contentType ==> " + contentType);
        if (TextUtils.isEmpty(contentType)) {
            return ResponseResult.FAILED("图片格式错误");
        }
        // 获取相关数据，比如说文件类型，文件名称
        String originalFilename = file.getOriginalFilename();
        log.info("originalFilename ==> " + originalFilename);
        String type = getType(contentType, originalFilename);
        if (type == null) {
            return ResponseResult.FAILED("不支持此文件类型");
        }
        // 限制文件大小
        long size = file.getSize();
        log.info("max-size ==> " + maxSize + "size ==> " + size);
        if (size > maxSize) {
            return ResponseResult.FAILED("图片最大支持" + (maxSize / 1024 / 1024) + "Mb");
        }
        // 创建图片保存目录
        // 规则：配置目录/日期/类型/ID.类型
        long currentMillions = System.currentTimeMillis();
        String currentDay = new SimpleDateFormat("yyyy_MM_dd").format(currentMillions);
        log.info("currentDay ==> " + currentDay);
        String dayPath = imagePath + File.separator + currentDay;
        File dayPathFile = new File(dayPath);
        // 判断日期文件夹是否存在  2023_01_08
        if (!dayPathFile.exists()) {
            dayPathFile.mkdirs();
        }
        String targetName = String.valueOf(idWorker.nextId());
        String targetPath = dayPath +
                File.separator + type + File.separator + targetName + "." + type;
        File targetFile = new File(targetPath);
        // 判断类型文件夹是否存在  gif
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        try {
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            log.info("targetFile ==> " + targetFile);
            // 保存文件
            file.transferTo(targetFile);
            // 返回结果：包含这个图片的名称和访问路径
            // 第一个是访问路径--> 得对应解析来
            Map<String, String> result = new HashMap<>();
            String resultPath = currentMillions + "_" + targetName + "." + type;
            result.put("id", resultPath);
            // 第二个是名称-->alt="图片描述"，如果不写前端可以使用名称作为这个描述
            result.put("name", originalFilename);
            Image image = new Image();
            image.setContentType(contentType);
            image.setId(targetName);
            image.setCreateTime(new Date());
            image.setUpdateTime(new Date());
            image.setPath(targetFile.getPath());
            image.setName(originalFilename);
            image.setUrl(resultPath);
            image.setState("1");
            image.setOriginal(original);
            InisUser inisUser = userService.checkInisUser();
            image.setUserId(inisUser.getId());
            // 记录文件
            // 保存记录到数据库
            imageDao.save(image);
            // 返回结果
            return ResponseResult.SUCCESS("图片上传成功").setData(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseResult.FAILED("图片上传失败，请稍后重试");
    }

    private String getType(String contentType, String name) {
        String type = null;
        if (Constants.ImageType.TYPE_PNG_WITH_PREFIX.equals(contentType)
                && name.endsWith(Constants.ImageType.TYPE_PNG)) {
            type = Constants.ImageType.TYPE_PNG;
        } else if (Constants.ImageType.TYPE_GIF_WITH_PREFIX.equals(contentType)
                && name.endsWith(Constants.ImageType.TYPE_GIF)) {
            type = Constants.ImageType.TYPE_GIF;
        } else if (Constants.ImageType.TYPE_JPG_WITH_PREFIX.equals(contentType)
                && name.endsWith(Constants.ImageType.TYPE_JPG)) {
            type = Constants.ImageType.TYPE_JPG;
        }
        return type;
    }

    @Override
    public void viewImage(HttpServletResponse response, String imageId) throws IOException {
        // 配置的目录已知
        // 根据尺寸来动态返回图片给前端
        // 好处：减少带宽占用，传输速度快
        // 缺点：消耗后台的CPU资源
        // 推荐做法：上传上来的时候，把图片复制成三个尺寸：大、中、小
        // 根据尺寸范围，返回结果即可
        // 需要日期
        String[] paths = imageId.split("_");
        String dayValue = paths[0];
        String format;
        format = new SimpleDateFormat("yyyy_MM_dd").format(Long.parseLong(dayValue));
        log.info("viewImage format ==> " + format);
        // ID
        String name = paths[1];
        // 需要类型
        String type = name.substring(name.length() - 3);
        // 使用日期的时间戳_ID.类型
        String targetPath = imagePath + File.separator + format + File.separator + type + File.separator + name;
        log.info("get image target path ==> " + targetPath);
        File file = new File(targetPath);
        OutputStream writer = null;
        FileInputStream fos = null;
        try {
            response.setContentType("image/png");
            writer = response.getOutputStream();
            // 读取
            fos = new FileInputStream(file);
            byte[] buff = new byte[1024];
            int len;
            while ((len = fos.read(buff)) != -1) {
                writer.write(buff, 0, len);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                fos.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public ResponseResult listImages(int page, int size, String original) {
        // 处理page、size
        page = checkPage(page);
        size = checkSize(size);
        InisUser inisUser = userService.checkInisUser();
        if (inisUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        // 创建分页条件
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        // 查询
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        // 返回结果
        final String userId = inisUser.getId();
        Page<Image> all = imageDao.findAll(new Specification<Image>() {
            @Override
            public Predicate toPredicate(Root<Image> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                //根据用户ID
                Predicate userIdPre = cb.equal(root.get("userId").as(String.class), userId);
                //根据状态
                Predicate statePre = cb.equal(root.get("state").as(String.class), "1");
                Predicate and;
                if (!TextUtils.isEmpty(original)) {
                    Predicate originalPre = cb.equal(root.get("original").as(String.class), original);
                    and = cb.and(userIdPre, statePre, originalPre);
                } else {
                    and = cb.and(userIdPre, statePre);
                }
                return and;
            }
        }, pageable);
        return ResponseResult.SUCCESS("获取图片列表成功").setData(all);
    }

    /**
     * 删除图片
     * 只改变状态
     * @param imageId
     * @return
     */
    @Override
    public ResponseResult deleteById(String imageId) {
        int result = imageDao.deleteImageByUpdateState(imageId);
        if (result>0) {
            return ResponseResult.SUCCESS("删除成功");
        }
        return ResponseResult.FAILED("删除失败");
    }


}
