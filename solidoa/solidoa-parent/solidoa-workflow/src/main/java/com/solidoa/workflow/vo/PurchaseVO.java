package com.solidoa.workflow.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购申请VO
 */
@Data
public class PurchaseVO {

    private Long id;
    private String purchaseNo;

    /** 申请人信息 */
    private Long userId;
    private String userName;
    private String deptName;

    /** 采购信息 */
    private String purchaseType;
    private String purchaseTypeDesc;

    /** @deprecated 与 items 列表中的字段冗余，请使用 items 获取明细 */
    @Deprecated
    private String itemName;
    /** @deprecated 与 items 列表中的字段冗余，请使用 items 获取明细 */
    @Deprecated
    private Integer quantity;
    /** @deprecated 与 items 列表中的字段冗余，请使用 items 获取明细 */
    @Deprecated
    private String unit;
    /** @deprecated 与 items 列表中的字段冗余，请使用 items 获取明细 */
    @Deprecated
    private BigDecimal budgetAmount;

    /** 供应商信息 */
    private String supplierName;
    private String supplierContact;
    private String supplierPhone;

    /** 其他信息 */
    private String reason;
    private List<String> attachmentUrls;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;

    /** 状态 */
    private String status;
    private String statusDesc;
    private String deliveryStatus;
    private String deliveryStatusDesc;

    /** 审批信息 */
    private Long currentApproverId;
    private String currentApproverName;

    /** 时间 */
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 采购明细 */
    private List<PurchaseItemVO> items;

    /** 进度记录 */
    private List<PurchaseProgressVO> progressRecords;

    /** 审批历史 */
    private List<ApprovalRecordVO> approvalRecords;

    /** 流程节点信息 */
    private List<ApprovalNodeVO> approvalNodes;
}