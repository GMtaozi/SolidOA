package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExceptionVO {
    private Long userId;
    private String userName;
    private String deptName;
    private LocalDate date;
    private String exceptionType; // LATE, EARLY_LEAVE, ABSENT
    private Integer minutes; // 迟到/早退分钟数
}