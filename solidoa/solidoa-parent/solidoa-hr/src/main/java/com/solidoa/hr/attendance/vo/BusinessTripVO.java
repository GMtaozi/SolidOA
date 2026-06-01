package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出差申请VO
 */
@Data
public class BusinessTripVO {
    private Long id;
    private String tripNo;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private String destination;
    private String tripType;
    private String tripTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String reason;
    private BigDecimal budgetAmount;
    private String budgetRemark;
    private String attachments;
    private String emergencyContact;
    private String emergencyPhone;
    private String status;
    private String statusName;
    private String processInstanceId;
    private Long currentApproverId;
    private String currentApproverName;
    private LocalDateTime createTime;
    private String createTimeStr;

    /** 补贴记录列表 */
    private List<SubsidyVO> subsidies;

    /** 总补贴金额 */
    private BigDecimal totalSubsidy;
}