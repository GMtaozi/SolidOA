package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 假期余额VO
 */
@Data
public class LeaveBalanceVO {
    private Long id;
    private Long userId;
    private String userName;
    private String leaveType;
    private String leaveTypeName;
    private Integer year;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal pendingDays;
    private BigDecimal transferredDays;
    private BigDecimal availableDays;
}