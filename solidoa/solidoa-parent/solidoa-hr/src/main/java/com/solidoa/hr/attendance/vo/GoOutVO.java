package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 外出申请VO
 */
@Data
public class GoOutVO {
    private Long id;
    private String outNo;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private LocalDate outDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String destination;
    private String reason;
    private String attachments;
    private String status;
    private String processInstanceId;
    private Long currentApproverId;
    private String approverName;
    private String approverComment;
    private LocalDateTime approvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}