package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.workflow.service.ReminderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflow/reminder")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            throw new com.solidoa.common.exception.BusinessException(401, "用户未登录");
        }
        return Long.parseLong(userId);
    }

    @PostMapping("/send")
    public Result<Void> sendReminder(HttpServletRequest request) {
        String businessType = request.getParameter("businessType");
        String businessIdStr = request.getParameter("businessId");
        if (businessType == null || businessType.isEmpty() || businessIdStr == null || businessIdStr.isEmpty()) {
            throw new com.solidoa.common.exception.BusinessException(400, "businessType 和 businessId 不能为空");
        }
        Long businessId = Long.parseLong(businessIdStr);
        reminderService.sendReminder(businessType, businessId, getUserId(request));
        return Result.success();
    }
}
