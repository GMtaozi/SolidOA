package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
    List<Contact> selectByDeptId(@Param("deptId") Long deptId);

    List<Contact> searchByName(@Param("keyword") String keyword);
}