package com.solidoa.system.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.system.form.UserForm;
import com.solidoa.system.vo.UserVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.system.service.UserService;
import com.solidoa.system.mapper.UserMapper;
import com.solidoa.system.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageVO<UserVO> pageList(PageDTO dto, String username, String realName) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<UserVO> records = userMapper.selectPageList(offset, dto.getPageSize(), username, realName);
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
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("mobile", user.getMobile());
        result.put("email", user.getEmail());
        result.put("deptId", user.getDeptId());
        result.put("status", user.getStatus());
        return result;
    }

    @Override
    @Transactional
    public Long create(UserForm form) {
        User existing = userMapper.selectByUsername(form.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(form, user);
        user.setPassword("$2a$10$xxx"); // 默认密码
        user.setCreateTime(java.time.LocalDateTime.now());
        userMapper.insert(user);

        return user.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UserForm form) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(form, user);
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id) {
        User user = new User();
        user.setId(id);
        user.setPassword("$2a$10$xxx"); // 默认密码 admin123
        userMapper.updateById(user);
    }
}