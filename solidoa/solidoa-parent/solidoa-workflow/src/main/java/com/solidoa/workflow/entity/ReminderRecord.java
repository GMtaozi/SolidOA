package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_reminder_record")
public class ReminderRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String businessType;
    private Long businessId;
    private Long approverId;
    private Integer reminderCount;
    private LocalDateTime lastReminderTime;
    private LocalDateTime createTime;
}