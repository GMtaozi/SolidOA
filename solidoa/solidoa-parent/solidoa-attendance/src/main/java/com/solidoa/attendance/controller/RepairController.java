package com.solidoa.attendance.controller;

import com.solidoa.common.result.Result;
import com.solidoa.attendance.form.RepairForm;
import com.solidoa.attendance.service.RepairService;
import com.solidoa.attendance.vo.RepairVO;
import com.solidoa.common.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance/repair")
public class RepairController {

    @Autowired
    private RepairService repairService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid RepairForm form,
                                 @RequestHeader("X-User-Id") Long userId) {
        return Result.success(repairService.create(form, userId));
    }

    @GetMapping
    public Result<PageVO<RepairVO>> list(@RequestHeader("X-User-Id") Long userId,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(repairService.pageList(userId, pageNum, pageSize));
    }

    @GetMapping("/pending")
    public Result<List<RepairVO>> getPending() {
        return Result.success(repairService.getPendingList());
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody Map<String, String> body,
                                @RequestHeader("X-User-Id") Long approverId) {
        repairService.approve(id, body.get("result"), approverId);
        return Result.success();
    }
}