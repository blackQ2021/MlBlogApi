package cn.xuanq.blog.dao;

import cn.xuanq.blog.pojo.InisUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserDao extends JpaRepository<InisUser, String>, JpaSpecificationExecutor<InisUser> {

    /**
     *根据用户名查找用户
     * @param userName
     * @return
     */
    InisUser findOneByUserName(String userName);

    /**
     * 通过邮箱查找
     * @param email
     * @return
     */
    InisUser findOneByEmail(String email);

//    InisUser findOneByEmailOrByUserName(String email, String userName);

    /**
     * 根据userId来 查找用户
     * @param userId
     * @return
     */
    InisUser findOneById(String userId);

    /**
     * 通过修改用户的状态来删除用户
     * @param userId
     * @return
     */
    @Modifying
    @Query(nativeQuery = true, value = "update `tb_user` set `state` = '0' where `id` = ?")
    int deleteUserByState(String userId);

    /**
     * 通过邮箱来更新密码
     * @param encode
     * @param email
     * @return
     */
    @Modifying
    @Query(nativeQuery = true, value = "update `tb_user` set `password` = ? where `email` = ?")
    int updatePasswordByEmail(String encode, String email);

    /**
     * 通过ID更新邮箱
     * @param email
     * @param id
     * @return
     */
    @Modifying
    @Query(nativeQuery = true, value = "update `tb_user` set `email` = ? where `id` = ?")
    int updateEmailById(String email, String id);
}
