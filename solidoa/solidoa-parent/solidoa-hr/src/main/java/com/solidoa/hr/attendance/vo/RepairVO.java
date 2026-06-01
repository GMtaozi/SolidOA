package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请VO
 */
@Data
public class RepairVO {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate repairDate;
    private String repairType;
    private String repairTypeName;
    private LocalDateTime repairTime;
    private LocalDateTime originalTime;
    private String reason;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private String approverComment;
    private LocalDateTime approvedTime;
    private LocalDateTime createTime;
}