package com.solidoa.workflow.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveVO {
    private Long id;
    private String leaveNo;
    private Long userId;
    private String userName;
    private String leaveType;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private BigDecimal hours;
    private String reason;
    private String attachments;
    private String status;
    private String statusName;
    private Long currentApproverId;
    private String currentApproverName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}