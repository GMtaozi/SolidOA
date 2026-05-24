package com.solidoa.system.service;

import com.solidoa.system.form.UserForm;
import com.solidoa.system.vo.UserVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.util.List;
import java.util.Map;

public interface UserService {
    PageVO<UserVO> pageList(PageDTO dto, String username, String realName);
    Map<String, Object> getById(Long id);
    Long create(UserForm form);
    void update(Long id, UserForm form);
    void delete(Long id);
    void resetPassword(Long id);
}