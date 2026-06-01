package com.solidoa.workflow.form;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 采购明细表单
 */
@Data
public class PurchaseItemForm {

    /** 物品名称 */
    @NotBlank(message = "请输入物品名称")
    private String itemName;

    /** 规格型号 */
    private String spec;

    /** 数量 */
    @NotNull(message = "请输入数量")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    /** 单位 */
    private String unit;

    /** 单价 */
    @NotNull(message = "请输入单价")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;

    /** 备注 */
    private String remark;
}