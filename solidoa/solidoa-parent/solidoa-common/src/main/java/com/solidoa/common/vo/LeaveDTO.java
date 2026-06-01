package com.solidoa.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LeaveDTO {
    private Long id;
    private String leaveNo;
    private Long userId;
    private String userName;
    private String leaveType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double days;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}