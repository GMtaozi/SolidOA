package com.solidoa.workflow.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 采购明细VO
 */
@Data
public class PurchaseItemVO {

    private Long id;
    private String itemName;
    private String spec;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String remark;
}