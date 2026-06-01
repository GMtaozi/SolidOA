package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 假期类型VO
 */
@Data
public class LeaveTypeVO {
    private Long id;
    private String leaveCode;
    private String leaveName;
    private BigDecimal defaultDays;
    private BigDecimal maxDays;
    private Integer requiresProof;
    private Integer canTransfer;
    private BigDecimal transferMaxDays;
    private Integer validMonths;
    private Integer deductSalary;
    private Integer sort;
    private Integer status;
}