package com.solidoa.attendance.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RepairVO {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate repairDate;
    private String repairType;
    private LocalDateTime repairTime;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}