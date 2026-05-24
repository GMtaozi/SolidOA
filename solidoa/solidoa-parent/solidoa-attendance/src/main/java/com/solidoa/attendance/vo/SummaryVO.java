package com.solidoa.attendance.vo;

import lombok.Data;

@Data
public class SummaryVO {
    private Integer workDays;
    private Integer actualDays;
    private Integer lateDays;
    private Integer earlyLeaveDays;
    private Integer leaveDays;
    private Integer absentDays;
    private Integer businessDays;
    private Double overtimeHours;
}