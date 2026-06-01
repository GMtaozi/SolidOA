package com.solidoa.file.service.impl;

import com.solidoa.common.client.WorkflowClient;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.ExpenseDTO;
import com.solidoa.common.vo.LeaveDTO;
import com.solidoa.file.entity.File;
import com.solidoa.file.service.FileAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件访问权限校验实现
 *
 * 权限规则：
 * 1. 上传者可以访问自己的文件
 * 2. 关联业务的其他参与者可以访问（通过 businessId + businessType 关联）
 */
@Service
@Slf4j
public class FileAccessServiceImpl implements FileAccessService {

    @Autowired
    private WorkflowClient workflowClient;

    @Override
    public boolean canAccess(File file, Long userId) {
        if (file == null || userId == null) {
            return false;
        }
        // 上传者可以访问
        if (file.getUploaderId() != null && file.getUploaderId().equals(userId)) {
            return true;
        }
        // 业务关联权限校验
        return hasBusinessAccess(file.getBusinessType(), file.getBusinessId(), userId);
    }

    /**
     * 检查用户是否通过业务关联获得文件访问权限
     */
    private boolean hasBusinessAccess(String businessType, Long businessId, Long userId) {
        if (businessType == null || businessId == null) {
            return false;
        }
        return switch (businessType) {
            case "LEAVE" -> checkLeaveAccess(businessId, userId);
            case "EXPENSE" -> checkExpenseAccess(businessId, userId);
            case "ATTENDANCE" -> checkAttendanceAccess(businessId, userId);
            default -> false;
        };
    }

    /**
     * 检查请假业务关联用户
     */
    private boolean checkLeaveAccess(Long leaveId, Long userId) {
        try {
            Result<LeaveDTO> result = workflowClient.getLeaveById(leaveId);
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                return false;
            }
            LeaveDTO leave = result.getData();
            return leave.getUserId() != null && leave.getUserId().equals(userId);
        } catch (Exception e) {
            log.warn("查询请假关联用户失败: leaveId={}, error={}", leaveId, e.getMessage());
            return false;
        }
    }

    /**
     * 检查报销业务关联用户
     */
    private boolean checkExpenseAccess(Long expenseId, Long userId) {
        try {
            Result<ExpenseDTO> result = workflowClient.getExpenseById(expenseId);
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                return false;
            }
            ExpenseDTO expense = result.getData();
            return expense.getUserId() != null && expense.getUserId().equals(userId);
        } catch (Exception e) {
            log.warn("查询报销关联用户失败: expenseId={}, error={}", expenseId, e.getMessage());
            return false;
        }
    }

    /**
     * 检查考勤业务关联用户
     */
    private boolean checkAttendanceAccess(Long attendanceId, Long userId) {
        try {
            Result<?> result = workflowClient.getLeaveSimple(attendanceId);
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                return false;
            }
            // 根据返回数据判断是否为考勤记录的拥有者
            Object data = result.getData();
            if (data instanceof com.solidoa.common.vo.LeaveDTO leave) {
                return leave.getUserId() != null && leave.getUserId().equals(userId);
            }
            return false;
        } catch (Exception e) {
            log.warn("查询考勤关联用户失败: attendanceId={}, error={}", attendanceId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canDownload(File file, Long userId) {
        return canAccess(file, userId);
    }

    @Override
    public boolean canPreview(File file, Long userId) {
        return canAccess(file, userId);
    }
}