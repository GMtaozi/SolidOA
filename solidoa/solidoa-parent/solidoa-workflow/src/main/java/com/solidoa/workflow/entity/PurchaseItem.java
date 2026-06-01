package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购明细
 */
@Data
@TableName("oa_purchase_item")
public class PurchaseItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购单ID */
    private Long purchaseId;

    /** 物品名称 */
    private String itemName;

    /** 规格型号 */
    private String spec;

    /** 数量 */
    private Integer quantity;

    /** 单位 */
    private String unit;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 总价 */
    private BigDecimal totalPrice;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 乐观锁版本号 */
    @Version
    private Long version;
}