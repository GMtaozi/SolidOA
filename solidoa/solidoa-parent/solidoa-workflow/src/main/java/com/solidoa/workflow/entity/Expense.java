package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_expense")
public class Expense {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String expenseNo;
    private Long userId;
    private Long deptId;
    private String expenseType;
    private BigDecimal amount;
    private String reason;
    private String attachments;
    private String bankName;
    private String bankAccount;
    private String status;
    private String processInstanceId;
    private Long currentApproverId;
    @Version
    private Integer version;
    private LocalDateTime paymentTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}