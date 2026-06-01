package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.system.form.DeptForm;
import com.solidoa.system.service.DepartmentService;
import com.solidoa.system.vo.DeptTreeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/depts")
public class DeptController {

    private final DepartmentService departmentService;

    public DeptController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public Result<List<DeptTreeVO>> getTree() {
        return Result.success(departmentService.getTreeList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<DeptTreeVO> getById(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'DEPT_MANAGER') or hasAuthority('DEPT_CREATE')")
    public Result<Long> create(@RequestBody @Valid DeptForm form) {
        return Result.success(departmentService.create(form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'DEPT_MANAGER') or hasAuthority('DEPT_UPDATE')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid DeptForm form) {
        departmentService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('DEPT_DELETE')")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success();
    }
}