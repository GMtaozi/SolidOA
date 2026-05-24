package com.solidoa.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_repair_card")
public class RepairCard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate repairDate;
    private String repairType; // SIGN_IN, SIGN_OUT
    private LocalDateTime repairTime;
    private String reason;
    private String attachments;
    private String status; // PENDING, APPROVED, REJECTED
    private Long approverId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}