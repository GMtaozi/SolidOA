package com.solidoa.workflow.service;

import com.solidoa.workflow.form.PurchaseForm;
import com.solidoa.workflow.form.PurchaseProgressForm;
import com.solidoa.workflow.form.ApproveForm;
import com.solidoa.workflow.vo.PurchaseVO;
import com.solidoa.workflow.vo.PurchaseStatisticsVO;
import java.util.List;

/**
 * 采购申请服务接口
 */
public interface PurchaseService {

    /**
     * 创建采购申请
     */
    Long createPurchase(PurchaseForm form, Long userId);

    /**
     * 采购申请列表
     */
    List<PurchaseVO> listPurchase(Long userId, String status, String purchaseType);

    /**
     * 采购申请详情
     */
    PurchaseVO getPurchaseById(Long id, Long userId);

    /**
     * 审批采购申请
     */
    void approvePurchase(Long id, ApproveForm form, Long approverId);

    /**
     * 撤回采购申请
     */
    void cancelPurchase(Long id, Long userId);

    /**
     * 更新采购进度
     */
    void updateProgress(Long id, PurchaseProgressForm form, Long operatorId);

    /**
     * 获取采购统计
     */
    PurchaseStatisticsVO getStatistics(Long deptId, String purchaseType);
}