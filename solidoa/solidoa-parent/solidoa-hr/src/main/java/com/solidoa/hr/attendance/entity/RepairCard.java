package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请实体
 * 补卡类型：MISSING（缺卡）、LATE（迟到）、EARLY_LEAVE（早退）
 */
@Data
@TableName("oa_repair_card")
public class RepairCard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate repairDate; // 补卡日期
    private String repairType; // 补卡类型：MISSING, LATE, EARLY_LEAVE
    private LocalDateTime repairTime; // 补卡时间
    private LocalDateTime originalTime; // 原本应该打卡的时间
    private String reason; // 补卡原因
    private String timeSlot; // 时间段：MORNING上班、NIGHT下班
    private String attachments; // 证明材料URL
    private String status; // 状态：PENDING, APPROVED, REJECTED
    private Long approverId; // 审批人ID
    private String approverComment; // 审批意见
    private LocalDateTime approvedTime; // 审批时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}