package com.solidoa.system.form;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduleForm {
    private String title;
    private String content;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isAllDay = 0;
    private Integer remindBefore = 15;
    private String remindWay = "APP";
    private String color = "#409EFF";
}