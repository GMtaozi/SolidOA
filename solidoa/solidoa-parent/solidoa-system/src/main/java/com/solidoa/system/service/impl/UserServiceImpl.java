package com.solidoa.system.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.security.UserContextHolder;
import com.solidoa.system.form.ChangePasswordForm;
import com.solidoa.system.form.UserForm;
import com.solidoa.system.vo.UserVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.system.service.UserService;
import com.solidoa.system.mapper.UserMapper;
import com.solidoa.system.mapper.UserRoleMapper;
import com.solidoa.system.mapper.DepartmentMapper;
import com.solidoa.system.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Value("${user.default-password:}")
    private String defaultPassword;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @PostConstruct
    public void validateDefaultPassword() {
        if (defaultPassword == null || defaultPassword.trim().isEmpty()) {
            throw new IllegalStateException(
                "【安全配置错误】默认密码未配置！请通过环境变量或配置中心设置 'user.default-password'。不允许使用空密码或默认值创建用户账户。"
            );
        }
        log.info("默认密码已配置，长度: {}", defaultPassword.length());
    }

    @Override
    public PageVO<UserVO> pageList(PageDTO dto, String username, String realName) {
        // 获取当前用户的信息
        String currentUserId = getCurrentUserId();
        String currentUserDeptId = getCurrentUserDeptId();
        List<String> currentUserRoles = getCurrentUserRoles();

        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<UserVO> records = userMapper.selectPageList(offset, dto.getPageSize(), username, realName);

        // 将 GROUP_CONCAT 的角色字符串转为列表
        records.forEach(vo -> {
            // MyBatis 将 GROUP_CONCAT 结果映射为 Object，需要手动转换
            Object rolesObj = vo.getRoles();
            if (rolesObj instanceof String) {
                String rolesStr = (String) rolesObj;
                vo.setRoles(Arrays.asList(rolesStr.split(",")));
            } else if (rolesObj == null) {
                vo.setRoles(Collections.emptyList());
            }
        });

        // 数据权限过滤：非管理员只能查看本部门及下级部门用户
        boolean isAdmin = currentUserRoles.stream()
                .anyMatch(r -> r.equalsIgnoreCase("admin") || r.equalsIgnoreCase("SYSTEM_ADMIN"));
        if (!isAdmin && currentUserDeptId != null) {
            // 获取当前用户部门及下级部门ID列表
            List<Long> deptIds = departmentMapper.selectChildDeptIds(Long.parseLong(currentUserDeptId));
            records = records.stream()
                    .filter(r -> r.getDeptId() != null && deptIds.contains(r.getDeptId()))
                    .toList();
        }

        long total = userMapper.selectCount(username, realName);

        PageVO<UserVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    public Map<String, Object> getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 数据权限：非管理员不能查看其他部门用户
        String currentUserId = getCurrentUserId();
        List<String> currentUserRoles = getCurrentUserRoles();
        String currentUserDeptId = getCurrentUserDeptId();

        if (!currentUserRoles.contains("admin") && !currentUserId.equals(String.valueOf(id))) {
            if (user.getDeptId() != null && currentUserDeptId != null) {
                List<Long> deptIds = departmentMapper.selectChildDeptIds(Long.parseLong(currentUserDeptId));
                if (!deptIds.contains(user.getDeptId())) {
                    throw new BusinessException("无权访问该用户");
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("mobile", maskMobile(user.getMobile()));
        result.put("email", maskEmail(user.getEmail()));
        result.put("deptId", user.getDeptId());
        result.put("status", user.getStatus());
        return result;
    }

    @Override
    public Map<String, Object> getCurrentUser() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException("无法获取当前用户身份，请重新登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> roles = userRoleMapper.selectRoleCodesByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("mobile", maskMobile(user.getMobile()));
        result.put("email", maskEmail(user.getEmail()));
        result.put("deptId", user.getDeptId());
        result.put("status", user.getStatus());
        result.put("roles", roles);
        return result;
    }

    @Override
    @Transactional
    public Long create(UserForm form) {
        User existing = userMapper.selectByUsername(form.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        // 验证手机号格式
        if (form.getMobile() != null && !form.getMobile().isEmpty()) {
            if (!PHONE_PATTERN.matcher(form.getMobile()).matches()) {
                throw new BusinessException("手机号格式不正确");
            }
        }

        User user = new User();
        BeanUtils.copyProperties(form, user);

        // 使用BCrypt加密默认密码
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setCreateTime(java.time.LocalDateTime.now());
        userMapper.insert(user);

        log.info("创建用户: id={}, username={}, 默认密码已加密", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserForm form) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证手机号格式
        if (form.getMobile() != null && !form.getMobile().isEmpty()) {
            if (!PHONE_PATTERN.matcher(form.getMobile()).matches()) {
                throw new BusinessException("手机号格式不正确");
            }
        }

        BeanUtils.copyProperties(form, user);
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 安全校验：禁止删除自己
        Long currentUserId = UserContextHolder.getUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new BusinessException("不能删除当前登录用户");
        }

        // 安全校验：禁止非管理员删除管理员账户
        User targetUser = userMapper.selectById(id);
        if (targetUser == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> currentUserRoles = getCurrentUserRoles();
        boolean isAdmin = currentUserRoles.contains("admin");

        if ("admin".equals(targetUser.getUsername()) && !isAdmin) {
            throw new BusinessException("不能删除系统管理员账户");
        }

        // 数据权限：非管理员只能删除本部门及下级部门用户
        if (!isAdmin) {
            String currentUserDeptId = getCurrentUserDeptId();
            if (currentUserDeptId != null) {
                List<Long> deptIds = departmentMapper.selectChildDeptIds(Long.parseLong(currentUserDeptId));
                if (targetUser.getDeptId() == null || !deptIds.contains(targetUser.getDeptId())) {
                    throw new BusinessException("无权删除该部门用户");
                }
            }
        }

        // 检查用户是否有关联数据
        if (userRoleMapper.selectRoleIdsByUserId(id).size() > 0) {
            throw new BusinessException("该用户存在角色关联，请先解除关联");
        }

        // 检查用户是否关联了钉钉
        if (targetUser.getDingtalkUserid() != null && !targetUser.getDingtalkUserid().isEmpty()) {
            throw new BusinessException("该用户已绑定钉钉，请先解除绑定");
        }

        userMapper.deleteById(id);
        log.info("删除用户: id={}", id);
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 生成新的随机密码
        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        log.info("重置用户密码: id={}, username={}", id, user.getUsername());
        return newPassword;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordForm form) {
        // 验证新密码和确认密码是否一致
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("新密码和确认密码不一致");
        }

        // 验证新密码长度
        if (form.getNewPassword().length() < 6 || form.getNewPassword().length() > 20) {
            throw new BusinessException("密码长度必须在6-20位之间");
        }

        // 获取当前登录用户
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码是否正确
        if (!passwordEncoder.matches(form.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户修改密码: id={}, username={}", userId, user.getUsername());
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String suffix = email.substring(atIndex);
        if (prefix.length() <= 2) {
            return prefix.charAt(0) + "***" + suffix;
        }
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + suffix;
    }

    private String generateRandomPassword() {
        SecureRandom secureRandom = new SecureRandom();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return "S" + sb.toString() + "@1";
    }

    private String getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? userId.toString() : null;
    }

    private String getCurrentUserDeptId() {
        Long deptId = UserContextHolder.getDeptId();
        return deptId != null ? deptId.toString() : null;
    }

    private List<String> getCurrentUserRoles() {
        return com.solidoa.common.security.UserContextHolder.getRolesList();
    }
}