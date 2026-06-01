package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    List<Role> selectListWithUserCount();

    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);
}