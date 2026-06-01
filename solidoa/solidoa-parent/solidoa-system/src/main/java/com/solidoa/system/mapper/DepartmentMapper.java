package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    List<Department> selectTreeList();

    Department selectByIdWithLeader(@Param("id") Long id);

    List<Long> selectChildIds(@Param("parentId") Long parentId);

    List<Long> selectChildDeptIds(@Param("parentId") Long parentId);
}