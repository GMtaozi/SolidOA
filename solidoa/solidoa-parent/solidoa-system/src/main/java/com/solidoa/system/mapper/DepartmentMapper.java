package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    List<Department> selectTreeList();
}