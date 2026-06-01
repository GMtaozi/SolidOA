package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.system.form.ChangePasswordForm;
import com.solidoa.system.form.UserForm;
import com.solidoa.system.vo.UserVO;
import com.solidoa.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'HR') or hasAuthority('USER_LIST')")
    public Result<PageVO<UserVO>> list(PageDTO dto,
                                       @RequestParam(value = "username", required = false) String username,
                                       @RequestParam(value = "realName", required = false) String realName) {
        return Result.success(userService.pageList(dto, username, realName));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'HR') or hasAuthority('USER_VIEW')")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/users/current")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getCurrentUser() {
        // 安全断言：确保身份上下文存在且已验证
        if (com.solidoa.common.security.UserContextHolder.getUserId() == null) {
            throw new com.solidoa.common.exception.BusinessException("用户身份未验证，请通过正规渠道登录");
        }
        return Result.success(userService.getCurrentUser());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('USER_CREATE')")
    public Result<Long> create(@RequestBody @Valid UserForm form) {
        return Result.success(userService.create(form));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('USER_UPDATE')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserForm form) {
        userService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('USER_DELETE')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/users/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('USER_RESET_PASSWORD')")
    public Result<String> resetPassword(@PathVariable Long id) {
        String newPassword = userService.resetPassword(id);
        return Result.success(newPassword);
    }

    @PutMapping("/users/current/password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordForm form) {
        userService.changePassword(form);
        return Result.success();
    }
}
