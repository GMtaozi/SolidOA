package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购申请实体
 */
@Data
@TableName("oa_purchase")
public class Purchase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购单号 */
    private String purchaseNo;

    /** 申请人ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /**
     * 采购类型
     * OFFICE-办公用品, IT-IT设备, FURNITURE-家具, SOFTWARE-软件服务, OTHER-其他
     */
    private String purchaseType;

    /** 物品名称 */
    private String itemName;

    /** 采购数量 */
    private Integer quantity;

    /** 单位 */
    private String unit;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 供应商名称 */
    private String supplierName;

    /** 供应商联系人 */
    private String supplierContact;

    /** 供应商电话 */
    private String supplierPhone;

    /** 采购原因 */
    private String reason;

    /** 附件 */
    private String attachments;

    /**
     * 审批状态
     * PENDING-审批中, APPROVED-已同意, REJECTED-已拒绝,
     * COMPLETED-已完成, CANCELLED-已撤回
     */
    private String status;

    /** Camunda流程实例ID */
    private String processInstanceId;

    /** 交付状态: PENDING-待采购, PURCHASING-采购中, DELIVERED-已到货 */
    private String deliveryStatus;

    /** 当前审批人ID */
    private Long currentApproverId;

    /** 期望交付日期 */
    private LocalDate expectedDeliveryDate;

    /** 实际交付日期 */
    private LocalDate actualDeliveryDate;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}