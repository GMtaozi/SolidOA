package com.solidoa.system.service;

import com.solidoa.system.form.RoleForm;
import com.solidoa.system.vo.RoleVO;
import java.util.List;

public interface RoleService {
    List<RoleVO> list();

    RoleVO getById(Long id);

    Long create(RoleForm form);

    void update(Long id, RoleForm form);

    void delete(Long id);

    List<String> getPermissions(Long roleId);

    void assignPermissions(Long roleId, List<Long> permissionIds);
}