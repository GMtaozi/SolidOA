package com.solidoa.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solidoa.system.entity.RolePermission;
import com.solidoa.system.mapper.RolePermissionMapper;
import com.solidoa.system.service.RolePermissionService;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>
        implements RolePermissionService {
}
