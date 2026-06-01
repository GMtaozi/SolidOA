package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_payment_log")
public class PaymentLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long expenseId;
    private Long cashierId;
    private BigDecimal amount;
    private String confirmNote;
    private LocalDateTime paymentTime;
    private LocalDateTime createTime;
}