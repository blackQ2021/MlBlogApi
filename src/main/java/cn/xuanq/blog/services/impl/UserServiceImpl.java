package cn.xuanq.blog.services.impl;

import cn.xuanq.blog.dao.RefreshTokenDao;
import cn.xuanq.blog.dao.SettingsDao;
import cn.xuanq.blog.dao.UserDao;
import cn.xuanq.blog.pojo.InisUser;
import cn.xuanq.blog.pojo.RefreshToken;
import cn.xuanq.blog.pojo.Setting;
import cn.xuanq.blog.response.ResponseResult;
import cn.xuanq.blog.response.ResponseState;
import cn.xuanq.blog.services.IUserService;
import cn.xuanq.blog.utils.*;
import com.google.gson.Gson;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.util.*;

@Slf4j
@Service
@Transactional
public class UserServiceImpl extends BaseService implements IUserService {

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private UserDao userDao;
    @Autowired
    private SettingsDao settingsDao;
    @Autowired
    private RefreshTokenDao refreshTokenDao;
    @Autowired
    private Gson gson;

    /**
     * 初始化管理员账户
     * @param inisUser
     * @param request
     * @return
     */
    @Override
    public ResponseResult initMangerAccount(InisUser inisUser, HttpServletRequest request) {
        //检查是否有初始化
        Setting mangerAccountState = settingsDao.findOneByKey(Constants.Settings.MANGER_ACCOUNT_INIT_STATE);
        if (mangerAccountState != null) {
            return ResponseResult.FAILED("管理员账号已初始化");
        }
        //TODO:
        //检查数据
        if (TextUtils.isEmpty(inisUser.getUserName())) {
            return ResponseResult.FAILED("用户名不能为空");
        }
        if (TextUtils.isEmpty(inisUser.getPassword())) {
            return  ResponseResult.FAILED("密码不能为空");
        }
        if (TextUtils.isEmpty(inisUser.getEmail())) {
            return  ResponseResult.FAILED("邮箱不能为空");
        }

        // 补充数据
        inisUser.setId(String.valueOf(idWorker.nextId()));
        inisUser.setRoles(Constants.User.ROLE_ADMIN);
        inisUser.setAvatar(Constants.User.DEFAULT_AVATAR);
        inisUser.setState(Constants.User.DEFAULT_STATE);
        inisUser.setLoginIp(request.getLocalAddr());
        inisUser.setRegIp(request.getRemoteAddr());
        inisUser.setCreateTime(new Date());
        inisUser.setUpdateTime(new Date());
        // 对密码进行加密
        // 原密码
        String password = inisUser.getPassword();
        // 加密码
        String encode = bCryptPasswordEncoder.encode(password);
        inisUser.setPassword(encode);
        // 保存到数据库
        userDao.save(inisUser);
        // 更新已经添加的标记
        // 肯定
        Setting setting = new Setting();
        setting.setId(idWorker.nextId() + "");
        setting.setKey(Constants.Settings.MANGER_ACCOUNT_INIT_STATE);
        setting.setCreateTime(new Date());
        setting.setUpdateTime(new Date());
        setting.setValue("1");
        settingsDao.save(setting);
        return  ResponseResult.SUCCESS("初始化成功");
    }

    @Autowired
    private Random random;

    @Autowired
    private RedisUtil redisUtil;
    public static final int[] captcha_font_types = {Captcha.FONT_1,
            Captcha.FONT_2,
            Captcha.FONT_3,
            Captcha.FONT_4,
            Captcha.FONT_5,
            Captcha.FONT_6,
            Captcha.FONT_7,
            Captcha.FONT_8,
            Captcha.FONT_9,
            Captcha.FONT_10};

    /**
     * 生成验证码，验证码key值存入redis
     * @param response
     * @param captchaKey
     * @throws Exception
     */
    @Override
    public void createCaptcha(HttpServletResponse response, String captchaKey) throws Exception{
        if (TextUtils.isEmpty(captchaKey) || captchaKey.length() < 13) {
            return;
        }
        long key;
        try {
            key = Long.parseLong(captchaKey);
        } catch (Exception e) {
            return;
        }
        // 可以用了
        // 设置请求头为输出图片类型
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        int captchaType = random.nextInt(3);
        Captcha targetCaptcha;
        if (captchaType == 0) {
            // 三个参数分别为宽、高、位数
            targetCaptcha = new SpecCaptcha(95, 32, 5);
        }else if (captchaType == 1) {
            // gif类型
            targetCaptcha = new GifCaptcha(95, 32);
        }else {
            // 算术类型
            targetCaptcha = new ArithmeticCaptcha(95, 32);
            targetCaptcha.setLen(2);  // 几位数运算，默认是两位
            // (ArithmeticCaptcha)targetCaptcha.getArithmeticString(); 获取运算的公式：3+2=?
            // targetCaptcha.text();  // 获取运算的结果：5
        }

        // 设置字体
        targetCaptcha.setFont(captcha_font_types[random.nextInt(captcha_font_types.length)]);
        // 设置类型，纯数字、纯字母、字母数字混合
        targetCaptcha.setCharType(Captcha.TYPE_DEFAULT);

        String content = targetCaptcha.text().toLowerCase();
        // 验证码存入session 与用户提交的验证码比对
//        request.getSession().setAttribute("captcha", content);
        log.info("content ==> " + content);
        // 保存到redis中
        // 删除时机
        // 1.自然过期，10分钟后自己删除
        // 2.验证码用完以后删除
        // 3.用完的情况：看get的地方
        redisUtil.set(Constants.User.KEY_CAPTCHA_CONTENT + key, content, 60 * 10);
        targetCaptcha.out(response.getOutputStream());
    }

    @Autowired
    private TaskService taskService;
    /**
     * 发送邮件验证码
     * 使用场聚:注册、找回密码、修改邮箱(会输入新的邮箱)
     * 注册（register）：如果已经法册过了，就提示说 该邮箱已经注册
     * 找回密码(forget):如果没有注册过,提示该邮箱没有注册
     * 修改邮箱(update)(新的邮箱):如果已经法册了、提示该邮箱已经注册
     * @param request
     * @param emailAddress
     * @return
     */
    @Override
    public ResponseResult sendEmail(String type, HttpServletRequest request, String emailAddress) {
        if (emailAddress == null) {
            return ResponseResult.FAILED("邮箱地址不可以为空");
        }
        // 根据类型查询邮箱是否存在
        if ("register".equals(type) || "update".equals(type)) {
            InisUser userByEmail = userDao.findOneByEmail(emailAddress);
            if (userByEmail != null) {
                return ResponseResult.FAILED("邮箱地址已注册");
            }
        }else if ("forget".equals(type)){
            InisUser userByEmail = userDao.findOneByEmail(emailAddress);
            if (userByEmail == null) {
                return ResponseResult.FAILED("该邮箱地址未注册");
            }
        }
        // 1、防止暴力发送 频率间隔30s 同一个IP，最多10次（短信最多5次）
        String remoteAddr = request.getRemoteAddr();
        log.info(remoteAddr);
        if (remoteAddr != null) {
            remoteAddr = remoteAddr.replaceAll(":","_");
        }
        // 拿出来 如果没有 就过
        String ipSendTimeValue = (String) redisUtil.get(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr);
        Integer ipSendTime;
        if (ipSendTimeValue != null) {
            ipSendTime = Integer.parseInt(ipSendTimeValue);
        } else {
            ipSendTime = 1;
        }
        if (ipSendTime > 10) {
            return ResponseResult.FAILED("验证码发送频繁");
        }
        Object hasEmailSend = redisUtil.get(Constants.User.KEY_EMAIL_SEND_ADDRESS + emailAddress);
        if (hasEmailSend != null) {
            return ResponseResult.FAILED("验证码发送频繁");
        }
        // 如果有就判断次数

        // 2、检查邮箱地址是否正确
        boolean isEmailFormatOk = TextUtils.isEmailAddressOk(emailAddress);
        if (!isEmailFormatOk) {
            return ResponseResult.FAILED("邮箱地址格式错误");
        }
        int code = random.nextInt(999999);
        if (code < 100000) {
            code += 100000;
        }
        log.info("" + code);
        // 3、发送验证码 6位数 100000-999999
        try {
            taskService.sendRegisterVerifyCode(String.valueOf(code), emailAddress);
        }catch (Exception e) {
            return ResponseResult.FAILED("发送失败，请稍后再试");
        }
        // 4、做记录
        // 发送记录 code
        if (ipSendTime == null) {
            ipSendTime = 0;
        }
        ipSendTime++;
        // 一个小时有效期
        redisUtil.set(Constants.User.KEY_EMAIL_SEND_IP + remoteAddr, String.valueOf(ipSendTime), 60*60);
        redisUtil.set(Constants.User.KEY_EMAIL_SEND_ADDRESS + emailAddress, "true", 30);
        // 保存code 10分钟内有效
        redisUtil.set(Constants.User.KEY_EMAIL_CODE_CONTENT + emailAddress, String.valueOf(code), 60 * 10);
        return ResponseResult.SUCCESS("验证码发送成功");
    }

    /**
     * 用户注册
     * @param inisUser
     * @param emailCode
     * @param captchaCode
     * @param captchaKey
     * @param request
     * @return
     */
    @Override
    public ResponseResult register(InisUser inisUser, String emailCode, String captchaCode, String captchaKey, HttpServletRequest request) {
        // 第一步：检查当前用户名是否已经注册
        String userName = inisUser.getUserName();
        if (TextUtils.isEmpty(userName)) {
            return ResponseResult.FAILED("用户名不能为空");
        }
        InisUser userByName = userDao.findOneByUserName(userName);
        if (userByName != null) {
            return ResponseResult.FAILED("该用户名已注册");
        }
        // 第二步：检查邮箱格式是否正确
        String email = inisUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            return ResponseResult.FAILED("邮箱地址不可以为空");
        }
        if (!TextUtils.isEmailAddressOk(email)) {
            return ResponseResult.FAILED("邮箱地址格式错误");
        }
        // 第三步：检查该邮箱是否已经注册
        InisUser userByEmail = userDao.findOneByEmail(email);
        if (userByEmail != null) {
            return ResponseResult.FAILED("邮箱地址已注册");
        }
        // 第四步：检查邮箱验证码是否正确
        String emailVerifyCode = (String) redisUtil.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (TextUtils.isEmpty(emailVerifyCode)) {
            return ResponseResult.FAILED("验证码无效");
        }
        if (!emailVerifyCode.equals(emailCode)) {
            return ResponseResult.FAILED("邮箱验证码不正确");
        } else {
            // 干掉里的内容
            redisUtil.del(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        }
        // 第五步：检查图灵验证码是否正确
        String captchaVerifyCode = (String) redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (TextUtils.isEmpty(captchaVerifyCode)) {
            return ResponseResult.FAILED("图灵验证码已过期");
        }
        if (!captchaVerifyCode.equals(captchaCode)) {
            return ResponseResult.FAILED("图灵验证码不正确");
        }else {
            redisUtil.del(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        }
        // 达到可以注册的条件
        // 第六步：对密码进行加密
        String password = inisUser.getPassword();
        if (TextUtils.isEmpty(password)) {
            return ResponseResult.FAILED("密码不能为空");
        }
        inisUser.setPassword(bCryptPasswordEncoder.encode(inisUser.getPassword()));
        // 第七步：补全数据
        // 包括：注册IP,登录IP,角色,头像,创建时间,更新时间
        String ipAddress = request.getRemoteAddr();
        inisUser.setRegIp(ipAddress);
        inisUser.setLoginIp(ipAddress);
        inisUser.setUpdateTime(new Date());
        inisUser.setCreateTime(new Date());
        inisUser.setAvatar(Constants.User.DEFAULT_AVATAR);
        inisUser.setRoles(Constants.User.ROLE_NORMAL);
        inisUser.setState("1");
        inisUser.setId(idWorker.nextId() + "");
        // 第八步：保存到数据库中
        userDao.save(inisUser);
        // 第九步：返回结果
        // 邮箱校验
        return ResponseResult.GET(ResponseState.JOIN_IN_SUCCESS);
    }

    /**
     * 用户登录
     * @param captcha
     * @param captchaKey
     * @param inisUser
     * @param request
     * @param response
     * @return
     */
    @Override
    public ResponseResult doLogin(String captcha,
                                  String captchaKey,
                                  InisUser inisUser,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        String captchaValue = (String) redisUtil.get(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        if (!captcha.equals(captchaValue)) {
            return ResponseResult.FAILED("图灵验证码不正确");
        }
        // 验证成功，删除redis里的验证码
        redisUtil.del(Constants.User.KEY_CAPTCHA_CONTENT + captchaKey);
        // 有可能是邮箱 也有可能是用户名
        String userName = inisUser.getUserName();
        if (TextUtils.isEmpty(userName)) {
            return ResponseResult.FAILED("用户账号不能为空");
        }
        String password = inisUser.getPassword();
        if (TextUtils.isEmpty(password)) {
            return ResponseResult.FAILED("密码不能为空");
        }

        InisUser userFromDb = userDao.findOneByUserName(userName);
        if (userFromDb == null) {
            userFromDb = userDao.findOneByEmail(userName);
        }
        if (userFromDb == null) {
            return ResponseResult.FAILED("用户名或密码错误");
        }
        // 用户存在
        // 对比密码
        boolean matches = bCryptPasswordEncoder.matches(password, userFromDb.getPassword());
        if (!matches) {
            return ResponseResult.FAILED("用户名或密码错误");
        }
        // 密码正确
        if (!"1".equals(userFromDb.getState())) {
            return ResponseResult.ACCOUNT_DENIED();
        }
        createToken(response, userFromDb);
        return ResponseResult.SUCCESS("登陆成功");
    }

    /**
     * 生成token，保存在redis中，返回tokenKey写到前端cookie里
     * @param response
     * @param userFromDb
     * @return
     */
    private String createToken(HttpServletResponse response, InisUser userFromDb) {
        int deleteResult = refreshTokenDao.deleteAllByUserId(userFromDb.getId());
        log.info("  " + deleteResult);
        // TODO:生成Token
        Map<String, Object> claims = ClaimsUtils.inisUser2Claims(userFromDb);
        // token默认两个小时
        String token = JwtUtil.createToken(claims);
        // 返回token的md5值，token保存在redis中
        // 前端访问的时候，携带token的md5key，从redis中获取即可
        String tokenKey = DigestUtils.md5DigestAsHex(token.getBytes());
        // 保存token到redis中
        redisUtil.set(Constants.User.KEY_TOKEN + tokenKey, token, Constants.TimeValueInSecond.HOUR_2);
        // 把tokenKey写到cookie里
        // 这个要动态获取，从request中获取，
        // TODO:工具类
        CookieUtils.setUpCookie(response, Constants.User.COOKIE_TOKEN_KEY, tokenKey);
        String refreshTokenValue = JwtUtil.createRefreshToken(userFromDb.getId(), Constants.TimeValueMillions.MONTH);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(idWorker.nextId() + "");
        refreshToken.setRefreshToken(refreshTokenValue);
        refreshToken.setUserId(userFromDb.getId());
        refreshToken.setTokenKey(tokenKey);
        refreshToken.setCreateTime(new Date());
        refreshToken.setUpdateTime(new Date());
        refreshTokenDao.save(refreshToken);
        return tokenKey;
    }

    private HttpServletRequest getRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request;
    }

    private HttpServletResponse getResponse() {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        return response;
    }

    /**
     * 通过用户ID查找用户信息
     * @param userId
     * @return
     */
    @Override
    public ResponseResult getUserInfo(String userId) {
        // 从数据库里获取
        InisUser user = userDao.findOneById(userId);
        // 判断结果
        if (user == null) {
            return ResponseResult.FAILED("用户不存在");
        }
        // 如果不存在就返回不存在
        String userJson = gson.toJson(user);
        InisUser newInisUser = gson.fromJson(userJson, InisUser.class);
        newInisUser.setPassword("");
        newInisUser.setEmail("");
        newInisUser.setRegIp("");
        newInisUser.setLoginIp("");
        // 如果存在就复制对象，清空密码、Email、登录ID、注册IP
        return ResponseResult.SUCCESS("获取成功").setData(newInisUser);
    }

    /**
     * 检查邮箱是否已经注册
     * @param email
     * @return
     */
    @Override
    public ResponseResult checkEmail(String email) {
        InisUser user = userDao.findOneByEmail(email);

        return user == null ? ResponseResult.FAILED("该邮箱未注册") : ResponseResult.SUCCESS("该邮箱已注册");
    }

    /**
     * 检查用户名是否已经注册
     * @param userName
     * @return
     */
    @Override
    public ResponseResult checkUserName(String userName) {
        InisUser user = userDao.findOneByUserName(userName);
        return user == null ? ResponseResult.FAILED("该用户名未注册") : ResponseResult.SUCCESS("该用户名已存在");
    }

    /**
     * 更新用户信息
     * @param userId
     * @param inisUser
     * @return
     */
    @Override
    public ResponseResult updateUserInfo(String userId, InisUser inisUser) {
        InisUser userFromTokenKey = checkInisUser();
        if (userFromTokenKey == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        InisUser userFromDb = userDao.findOneById(userFromTokenKey.getId());
        // 判断用户ID是否一致，可以去修改
        if (!userFromDb.getId().equals(userId)) {
            return ResponseResult.PERMISSION_DENIED();
        }
        // 修改
        String userName = inisUser.getUserName();
        if (!TextUtils.isEmpty(userName) && !userName.equals(userFromTokenKey.getUserName())) {
            InisUser userByUserName = userDao.findOneByUserName(userName);
            if (userByUserName != null) {
                return ResponseResult.FAILED("该用户名已存在");
            }
            userFromDb.setUserName(userName);
        }

        if (!TextUtils.isEmpty(inisUser.getAvatar())) {
            userFromDb.setAvatar(inisUser.getAvatar());
        }
        userFromDb.setSign(inisUser.getSign());

        userFromDb.setUpdateTime(new Date());
        userDao.save(userFromDb);
        // 干掉redis里的token，下一次请求
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);
        redisUtil.del(Constants.User.KEY_TOKEN + tokenKey);
        return ResponseResult.SUCCESS("用户修改成功");
    }

    /**
     * 删除用户，并不是真的删除
     * 修改状态
     * @param userId
     * @return
     */
    @Override
    public ResponseResult deleteUserById(String userId) {
        // 可以操作
        int result = userDao.deleteUserByState(userId);
        if (result > 0) {
            return ResponseResult.SUCCESS("删除成功");
        }
        return ResponseResult.FAILED("用户不存在");
    }

    /**
     * 管理员权限：获取用户列表
     * @param page
     * @param size
     * @return
     */
    @Override
    public ResponseResult listUsers(int page, int size, String userName, String email) {
        // 分页查询
        page = checkPage(page);
        // size限制一下，每一页不少于5个
        size = checkSize(size);
        // 根据注册日期排序
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(page-1, size, sort);
        Page<InisUser> all = userDao.findAll(new Specification<InisUser>() {
            // 实现条件查询 ？mybatis
            @Override
            public Predicate toPredicate(Root<InisUser> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (!TextUtils.isEmpty(userName)) {
                    Predicate preUser = cb.like(root.get("userName").as(String.class), "%" + userName + "%");
                    predicates.add(preUser);
                }
                if (!TextUtils.isEmpty(email)) {
                    Predicate preEmail = cb.equal(root.get("email").as(String.class),  email);
                    predicates.add(preEmail);
                }
                Predicate[] preArray = new Predicate[predicates.size()];
                predicates.toArray(preArray);
                return cb.and(preArray);
            }
        }, pageable);
        return ResponseResult.SUCCESS("获取用户列表成功").setData(all);
    }

    /**
     * 重置密码
     * @param userId
     * @param password
     * @return
     */
    @Override
    public ResponseResult resetPassword(String userId, String password) {
        //        查询出用户
        InisUser user = userDao.findOneById(userId);
        //        判断是否存在
        if (user == null) {
            return ResponseResult.FAILED("用户不存在");
        }
        //        密码加密
        user.setPassword(bCryptPasswordEncoder.encode(password));
        //        处理结果
        userDao.save(user);
        return ResponseResult.SUCCESS("重置成功");
    }

    /**
     * 更新密码
     * @param verifyCode
     * @param inisUser
     * @return
     */
    @Override
    public ResponseResult updateUserPassword(String verifyCode, InisUser inisUser) {
        // 检查邮箱是否填写
        String email = inisUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            return ResponseResult.FAILED("邮箱不可以为空");
        }
        // 根据邮箱去redis里那验证码
        String redisVerifyCode = (String) redisUtil.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (redisVerifyCode == null || !redisVerifyCode.equals(verifyCode)) {
            return ResponseResult.FAILED("验证码错误");
        }
        // 进行比对
        redisUtil.del(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        int result = userDao.updatePasswordByEmail(bCryptPasswordEncoder.encode(inisUser.getPassword()), email);

        return result > 0 ? ResponseResult.SUCCESS("密码修改成功") : ResponseResult.FAILED("密码修改失败");
    }

    /**
     * 更新邮箱
     * @param email
     * @param verifyCode
     * @return
     */
    @Override
    public ResponseResult updateEmail(String email, String verifyCode) {
        // 确保用户已经登录
        InisUser inisUser = this.checkInisUser();
        if (inisUser == null) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        // 对比验证码
        String redisVerifyCode = (String) redisUtil.get(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        if (TextUtils.isEmpty(redisVerifyCode) || !redisVerifyCode.equals(verifyCode)) {
            return ResponseResult.FAILED("验证码错误");
        }
        // 校验验证码正确，删除验证码
        redisUtil.del(Constants.User.KEY_EMAIL_CODE_CONTENT + email);
        // 可以修改
        int result = userDao.updateEmailById(email, inisUser.getId());
        return result > 0 ? ResponseResult.SUCCESS("邮箱修改成功") : ResponseResult.FAILED("邮箱修改失败");
    }

    /**
     * 退出登录
     * @return
     */
    @Override
    public ResponseResult doLogout() {
        // 拿到token_key
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);
        if (TextUtils.isEmpty(tokenKey)) {
            return ResponseResult.ACCOUNT_NOT_LOGIN();
        }
        // 删除redis里的token
        redisUtil.del(Constants.User.KEY_TOKEN + tokenKey);
        // 删除mysql里的refreshToken
        refreshTokenDao.deleteAllByTokenKey(tokenKey);
        // 删除cookie里的token_key
        CookieUtils.deleteCookie(getResponse(), Constants.User.COOKIE_TOKEN_KEY);
        return ResponseResult.SUCCESS("退出登录成功");
    }

    /**
     * 通过token，判断用户是否已经登录
     * @return
     */
    @Override
    public ResponseResult parseToken() {
        InisUser inisUser = checkInisUser();
        if (inisUser == null) {
            return ResponseResult.FAILED("用户未登录");
        }
        return ResponseResult.SUCCESS("用户获取成功").setData(inisUser);
    }

    /**
     *本质：通过携带的token_key检查用户是否有登陆
     * @return
     */
    @Override
    public InisUser checkInisUser() {
        //拿到token_key
        String tokenKey = CookieUtils.getCookie(getRequest(), Constants.User.COOKIE_TOKEN_KEY);

        InisUser inisUser = parseByTokenKey(tokenKey);
        if (inisUser == null) {
            // 根据解析出错了，过期了
            RefreshToken refreshToken = refreshTokenDao.findOneByTokenKey(tokenKey);
            if (refreshToken == null) {
                return null;
            }
            try {
                JwtUtil.parseJWT(refreshToken.getRefreshToken());
                String userId = refreshToken.getUserId();
                InisUser userFromDb = userDao.findOneById(userId);
                refreshTokenDao.deleteById(refreshToken.getId());
                String newTokenKey = createToken(getResponse(), userFromDb);
                return  parseByTokenKey(newTokenKey);
            }catch (Exception e1) {
                return null;
            }
        }
        return inisUser;
    }

    /**
     * 通过tokenKey在redis查找对应用户token
     * @param tokenKey
     * @return
     */
    private InisUser parseByTokenKey(String tokenKey) {
        String token = (String) redisUtil.get(Constants.User.KEY_TOKEN + tokenKey);
        if (token != null) {
            try {
                Claims claims = JwtUtil.parseJWT(token);
                return ClaimsUtils.claims2InisUser(claims);
            }catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

