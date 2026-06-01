package com.solidoa.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_schedule")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer isAllDay = 0;

    private Integer remindBefore = 15;

    private String remindWay = "APP";

    private String color = "#409EFF";

    private Long userId;

    private String status = "NORMAL";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}