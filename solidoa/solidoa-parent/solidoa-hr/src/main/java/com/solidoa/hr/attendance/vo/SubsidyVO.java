package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出差补贴VO
 */
@Data
public class SubsidyVO {
    private Long id;
    private Long tripId;
    private String subsidyType;
    private String subsidyTypeName;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createTime;
}