package cn.xuanq.blog.dao;

import cn.xuanq.blog.pojo.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LabelDao extends JpaRepository<Label, String>, JpaSpecificationExecutor<Label> {

    @Modifying
    int deleteOneById(String id);

    @Modifying
    @Query(value = "delete from 'tb_labels' where id = ?", nativeQuery = true)
    int customDeleteLabelById(String id);

    /**
     * 根据ID查找找一个标签
     * @param id
     * @return
     */
    Label findOneById(String id);

    Label findOneByName(String label);

    @Modifying
    @Query(nativeQuery = true, value = "update `tb_labels` set `count` = `count` + 1 where `name` = ?")
    int updateCountByName(String labelName);
}
