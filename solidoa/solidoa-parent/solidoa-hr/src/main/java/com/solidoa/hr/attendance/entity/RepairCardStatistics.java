package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 补卡次数统计实体（按月统计，每月从1日起算）
 */
@Data
@TableName("oa_repair_card_statistics")
public class RepairCardStatistics {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @TableField("ym")
    private String yearMonth; // 格式：yyyy-MM
    private Integer repairCount; // 补卡次数
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}