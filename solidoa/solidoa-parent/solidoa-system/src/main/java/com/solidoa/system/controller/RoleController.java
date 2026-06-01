package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.system.form.PermissionAssignForm;
import com.solidoa.system.form.RoleForm;
import com.solidoa.system.service.RoleService;
import com.solidoa.system.vo.RoleVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('ROLE_CREATE')")
    public Result<Long> create(@RequestBody @Valid RoleForm form) {
        return Result.success(roleService.create(form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('ROLE_UPDATE')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid RoleForm form) {
        roleService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('ROLE_DELETE')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("isAuthenticated()")
    public Result<List<String>> getPermissions(@PathVariable Long id) {
        return Result.success(roleService.getPermissions(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('ROLE_ASSIGN_PERMISSIONS')")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody @Valid PermissionAssignForm form) {
        roleService.assignPermissions(id, form.getPermissionIds());
        return Result.success();
    }
}