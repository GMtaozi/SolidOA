package com.solidoa.hr.attendance.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 调休余额VO
 */
@Data
public class OvertimeBalanceVO {
    private Long userId;
    private BigDecimal totalAvailable;
    private BigDecimal totalUsed;
    private BigDecimal totalExpired;
    private BigDecimal balance;
    private Integer breakCount;
}