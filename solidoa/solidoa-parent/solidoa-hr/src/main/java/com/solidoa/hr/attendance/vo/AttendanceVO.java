package com.solidoa.hr.attendance.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AttendanceVO {
    private Long id;
    private String checkType;
    private LocalDateTime checkTime;
    private String location;
    private Boolean isLate;
    private Boolean isEarlyLeave;
}