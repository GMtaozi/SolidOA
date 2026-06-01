package com.solidoa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.system.entity.Role;
import com.solidoa.system.entity.RolePermission;
import com.solidoa.system.form.RoleForm;
import com.solidoa.system.mapper.RoleMapper;
import com.solidoa.system.mapper.RolePermissionMapper;
import com.solidoa.system.service.RolePermissionService;
import com.solidoa.system.service.RoleService;
import com.solidoa.system.vo.RoleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PERMISSION_CACHE_KEY = "role:perms:";

    @Override
    public List<RoleVO> list() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<>());
        return roles.stream().map(this::convertToVO).toList();
    }

    @Override
    public RoleVO getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return convertToVO(role);
    }

    @Override
    @Transactional
    public Long create(RoleForm form) {
        Role role = new Role();
        BeanUtils.copyProperties(form, role);
        roleMapper.insert(role);
        log.info("创建角色: {}", role.getName());
        return role.getId();
    }

    @Override
    @Transactional
    public void update(Long id, RoleForm form) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        BeanUtils.copyProperties(form, role);
        roleMapper.updateById(role);
        redisTemplate.delete(PERMISSION_CACHE_KEY + id);
        log.info("更新角色: {}", role.getName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
        redisTemplate.delete(PERMISSION_CACHE_KEY + id);
        log.info("删除角色: {}", id);
    }

    @Override
    public List<String> getPermissions(Long roleId) {
        String cached = redisTemplate.opsForValue().get(PERMISSION_CACHE_KEY + roleId);
        if (cached != null) {
            return cached.isEmpty() ? List.of() : List.of(cached.split(","));
        }

        List<String> permissions = roleMapper.selectPermissionCodesByRoleId(roleId);
        redisTemplate.opsForValue().set(PERMISSION_CACHE_KEY + roleId,
            String.join(",", permissions), 30, TimeUnit.MINUTES);
        return permissions;
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 使用Redis分布式锁防止并发竞态
        String lockKey = PERMISSION_CACHE_KEY + "lock:" + roleId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(acquired)) {
            try {
                // 删除角色原有权限关联
                rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId));
                // 批量插入新的权限关联
                if (permissionIds != null && !permissionIds.isEmpty()) {
                    List<RolePermission> list = new ArrayList<>(permissionIds.size());
                    for (Long permissionId : permissionIds) {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permissionId);
                        list.add(rp);
                    }
                    rolePermissionService.saveBatch(list);
                }
                // 清除缓存
                redisTemplate.delete(PERMISSION_CACHE_KEY + roleId);
                log.info("分配权限到角色: roleId={}, permissionCount={}", roleId,
                        permissionIds == null ? 0 : permissionIds.size());
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            throw new BusinessException("角色权限分配正在进行中，请稍后重试");
        }
    }

    private RoleVO convertToVO(Role role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}