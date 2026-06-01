package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加班申请VO
 */
@Data
public class OvertimeVO {
    private Long id;
    private String overtimeNo;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private String overtimeType;
    private String overtimeTypeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal hours;
    private String reason;
    private String compensationType;
    private String compensationTypeName;
    private String status;
    private String statusName;
    private String processInstanceId;
    private Long currentApproverId;
    private String currentApproverName;
    private BigDecimal approvedHours;
    private LocalDateTime createTime;
    private String createTimeStr;
}