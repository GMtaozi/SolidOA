package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.system.form.UserForm;
import com.solidoa.system.vo.UserVO;
import com.solidoa.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public Result<PageVO<UserVO>> list(PageDTO dto,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String realName) {
        return Result.success(userService.pageList(dto, username, realName));
    }

    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping("/users")
    public Result<Long> create(@RequestBody @Valid UserForm form) {
        return Result.success(userService.create(form));
    }

    @PutMapping("/users/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserForm form) {
        userService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }
}