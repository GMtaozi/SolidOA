package com.solidoa.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出差补贴记录实体
 */
@Data
@TableName("oa_business_trip_subsidy")
public class BusinessTripSubsidy {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出差记录ID */
    private Long tripId;

    /** 补贴类型: TRANSPORT, ACCOMMODATION, DAILY */
    private String subsidyType;

    /** 补贴金额 */
    private BigDecimal amount;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}