package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.workflow.form.StampForm;
import com.solidoa.workflow.form.StampRecordForm;
import com.solidoa.workflow.form.ApproveForm;
import com.solidoa.workflow.vo.StampVO;
import com.solidoa.workflow.vo.StampStatisticsVO;
import com.solidoa.workflow.entity.StampUsageRecord;
import com.solidoa.workflow.mapper.StampUsageRecordMapper;
import com.solidoa.workflow.service.StampService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflow/stamp")
@Slf4j
public class StampController {

    @Autowired
    private StampService stampService;

    @Autowired
    private StampUsageRecordMapper stampUsageRecordMapper;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid StampForm form,
                              @RequestHeader("X-User-Id") Long userId) {
        Long result = stampService.createStamp(form, userId);
        return Result.success(result);
    }

    @GetMapping
    public Result<List<StampVO>> list(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam(required = false) String status) {
        List<StampVO> result = stampService.listStamp(userId, status);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<StampVO> getById(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId) {
        StampVO result = stampService.getStampById(id, userId);
        return Result.success(result);
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody ApproveForm form,
                                @RequestHeader("X-User-Id") Long approverId) {
        stampService.approveStamp(id, form, approverId);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                              @RequestHeader("X-User-Id") Long userId) {
        stampService.cancelStamp(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/record")
    public Result<Void> record(@PathVariable Long id,
                              @RequestBody StampRecordForm form,
                              @RequestHeader("X-User-Id") Long operatorId) {
        stampService.recordStamp(id, form, operatorId);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<StampStatisticsVO> statistics(@RequestParam(required = false) Long deptId) {
        StampStatisticsVO result = stampService.getStatistics(deptId);
        return Result.success(result);
    }

    @GetMapping("/types")
    public Result<List> types() {
        List types = Arrays.asList(
            Map.of("value", "PUBLIC", "label", "公章"),
            Map.of("value", "CONTRACT", "label", "合同章"),
            Map.of("value", "FINANCIAL", "label", "财务章"),
            Map.of("value", "LEGAL", "label", "法人章")
        );
        return Result.success(types);
    }

    /**
     * 查询用印记录
     */
    @GetMapping("/records")
    public Result<List<StampUsageRecord>> records(
            @RequestParam(required = false) String stampType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<StampUsageRecord> records = stampUsageRecordMapper.selectList(null);
        return Result.success(records);
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("StampService: " + (stampService != null ? "OK" : "NULL"));
    }
}
