package com.solidoa.workflow.form;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购申请表单
 */
@Data
public class PurchaseForm {

    /** 采购类型: OFFICE-办公用品, IT-IT设备, FURNITURE-家具, SOFTWARE-软件服务, OTHER-其他 */
    @NotBlank(message = "请选择采购类型")
    private String purchaseType;

    /** 需求部门ID */
    private Long requesterDeptId;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 物品名称 */
    @NotBlank(message = "请输入物品名称")
    private String itemName;

    /** 规格型号 */
    private String spec;

    /** 采购数量 */
    @NotNull(message = "请输入采购数量")
    @Min(value = 1, message = "采购数量至少为1")
    private Integer quantity;

    /** 单位 */
    private String unit;

    /** 预算金额 */
    @NotNull(message = "请输入预算金额")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal budgetAmount;

    /** 供应商名称 */
    private String supplierName;

    /** 供应商联系人 */
    private String supplierContact;

    /** 供应商电话 */
    private String supplierPhone;

    /** 采购原因 */
    private String reason;

    /** 期望交付日期 */
    private LocalDate expectedDeliveryDate;

    /** 附件列表 */
    private List<String> attachments;

    /** 采购明细 */
    private List<PurchaseItemForm> items;
}