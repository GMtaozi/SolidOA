package com.solidoa.attendance.vo;

import lombok.Data;

@Data
public class AttendanceVO {
    private Long id;
    private String checkType;
    private String checkTime;
    private String location;
    private Integer isLate;
    private Integer isEarlyLeave;
}