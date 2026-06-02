package com.solidoa.system.controller;

import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.system.entity.OperLog;
import com.solidoa.system.service.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志 (V2.0 5.3)
 */
@RestController
@RequestMapping("/api/v1/system/oper-logs")
public class OperLogController {

    @Autowired
    private OperLogService operLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN') or hasAuthority('OPER_LOG_VIEW')")
    public Result<PageVO<OperLog>> pageList(
        PageDTO dto,
        @RequestParam(required = false) String module,
        @RequestParam(required = false) String userName) {
        return Result.success(operLogService.pageList(dto, module, userName));
    }
}
