package com.solidoa.workflow.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.workflow.service.TaskService;
import com.solidoa.workflow.vo.TaskVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflow/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            throw new com.solidoa.common.exception.BusinessException(401, "用户未登录");
        }
        return Long.parseLong(userId);
    }

    private PageDTO getPageDTO(HttpServletRequest request) {
        PageDTO dto = new PageDTO();
        String pageNum = request.getParameter("pageNum");
        String pageSize = request.getParameter("pageSize");
        if (pageNum != null) dto.setPageNum(Integer.parseInt(pageNum));
        if (pageSize != null) dto.setPageSize(Integer.parseInt(pageSize));
        return dto;
    }

    @GetMapping("/my")
    public Result<PageVO<TaskVO>> getMyTasks(HttpServletRequest request) {
        return Result.success(taskService.getMyTasks(getUserId(request), getPageDTO(request)));
    }

    @GetMapping("/pending")
    public Result<PageVO<TaskVO>> getPendingTasks(HttpServletRequest request) {
        return Result.success(taskService.getPendingTasks(getUserId(request), getPageDTO(request)));
    }

    @GetMapping("/processed")
    public Result<PageVO<TaskVO>> getProcessedTasks(HttpServletRequest request) {
        return Result.success(taskService.getProcessedTasks(getUserId(request), getPageDTO(request)));
    }
}
