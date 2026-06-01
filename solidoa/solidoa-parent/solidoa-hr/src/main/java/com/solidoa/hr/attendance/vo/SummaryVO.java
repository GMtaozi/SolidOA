package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SummaryVO {
    private Long userId;
    private String yearMonth;
    private Integer workDays;
    private Integer actualDays;
    private Integer lateDays;
    private Integer earlyLeaveDays;
    private Integer leaveDays;
    private Integer absentDays;
    private Integer businessDays;
    private BigDecimal overtimeHours;
}