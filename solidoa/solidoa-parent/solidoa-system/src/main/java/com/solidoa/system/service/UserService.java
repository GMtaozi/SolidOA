package com.solidoa.system.service;

import com.solidoa.system.form.ChangePasswordForm;
import com.solidoa.system.form.UserForm;
import com.solidoa.system.vo.UserVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import java.util.List;
import java.util.Map;

public interface UserService {
    PageVO<UserVO> pageList(PageDTO dto, String username, String realName);
    Map<String, Object> getById(Long id);
    Map<String, Object> getCurrentUser();
    Long create(UserForm form);
    void update(Long id, UserForm form);
    void delete(Long id);
    String resetPassword(Long id);
    void changePassword(ChangePasswordForm form);
}