package com.solidoa.system.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduleVO {
    private Long id;
    private String title;
    private String content;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isAllDay;
    private Integer remindBefore;
    private String remindWay;
    private String color;
    private Long userId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}