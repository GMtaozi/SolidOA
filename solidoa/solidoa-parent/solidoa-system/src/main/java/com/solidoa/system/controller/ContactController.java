package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.system.form.ContactForm;
import com.solidoa.system.vo.ContactVO;
import com.solidoa.system.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/contacts")
@Validated
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public Result<Long> create(@RequestBody ContactForm form) {
        return Result.success(contactService.create(form));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ContactForm form) {
        contactService.update(id, form);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ContactVO> getById(@PathVariable Long id) {
        return Result.success(contactService.getById(id));
    }

    @GetMapping
    public Result<List<ContactVO>> list() {
        return Result.success(contactService.listAll());
    }

    @GetMapping("/dept/{deptId}")
    public Result<List<ContactVO>> listByDept(@PathVariable Long deptId) {
        return Result.success(contactService.listByDeptId(deptId));
    }

    @GetMapping("/search")
    public Result<PageVO<ContactVO>> search(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.success(contactService.search(keyword, pageNum, pageSize));
    }
}