package com.solidoa.hr.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("oa_salary_notice")
public class SalaryNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long salaryId;
    private Long userId;
    private String sendWay;
    private String sendStatus;
    private LocalDateTime sendTime;
    private LocalDateTime readTime;
    private String failReason;
    private LocalDateTime createTime;
}