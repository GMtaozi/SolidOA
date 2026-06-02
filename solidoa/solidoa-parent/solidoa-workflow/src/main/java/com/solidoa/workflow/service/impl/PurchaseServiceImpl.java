package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.workflow.entity.*;
import com.solidoa.workflow.form.*;
import com.solidoa.workflow.mapper.*;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.service.PurchaseService;
import com.solidoa.workflow.service.ApprovalNodeService;
import com.solidoa.workflow.service.UniversalApprovalService;
import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购申请服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final PurchaseItemMapper purchaseItemMapper;
    private final PurchaseProgressMapper progressMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalNodeService approvalNodeService;
    private final UniversalApprovalService universalService;

    /** 采购类型映射 */
    private static final Map<String, String> PURCHASE_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> STATUS_MAP = new HashMap<>();
    private static final Map<String, String> DELIVERY_STATUS_MAP = new HashMap<>();

    static {
        PURCHASE_TYPE_MAP.put("OFFICE", "办公用品");
        PURCHASE_TYPE_MAP.put("IT", "IT设备");
        PURCHASE_TYPE_MAP.put("FURNITURE", "家具");
        PURCHASE_TYPE_MAP.put("SOFTWARE", "软件/服务");
        PURCHASE_TYPE_MAP.put("OTHER", "其他");

        STATUS_MAP.put("PENDING", "审批中");
        STATUS_MAP.put("APPROVED", "已同意");
        STATUS_MAP.put("REJECTED", "已拒绝");
        STATUS_MAP.put("COMPLETED", "已完成");
        STATUS_MAP.put("CANCELLED", "已撤回");

        DELIVERY_STATUS_MAP.put("PENDING", "待采购");
        DELIVERY_STATUS_MAP.put("PURCHASING", "采购中");
        DELIVERY_STATUS_MAP.put("DELIVERED", "已到货");
        DELIVERY_STATUS_MAP.put("COMPLETED", "已完成");
    }

    @Override
    @Transactional
    public Long createPurchase(PurchaseForm form, Long userId) {
        // 1. 生成采购单号 CG + 年月日 + 序号
        String purchaseNo = generatePurchaseNo();

        // 2. 创建采购申请
        Purchase purchase = new Purchase();
        purchase.setPurchaseNo(purchaseNo);
        purchase.setUserId(userId);
        purchase.setPurchaseType(form.getPurchaseType());
        purchase.setItemName(form.getItemName());
        purchase.setQuantity(form.getQuantity());
        purchase.setUnit(form.getUnit());
        purchase.setBudgetAmount(form.getBudgetAmount());
        purchase.setSupplierName(form.getSupplierName());
        purchase.setSupplierContact(form.getSupplierContact());
        purchase.setSupplierPhone(form.getSupplierPhone());
        purchase.setReason(form.getReason());
        purchase.setAttachments(form.getAttachments() != null ? String.join(",", form.getAttachments()) : null);
        purchase.setExpectedDeliveryDate(form.getExpectedDeliveryDate());
        purchase.setStatus("PENDING");
        purchase.setDeliveryStatus("PENDING");
        purchase.setCreateTime(LocalDateTime.now());
        purchase.setUpdateTime(LocalDateTime.now());

        purchaseMapper.insert(purchase);

        // 3. 保存采购明细
        if (form.getItems() != null && !form.getItems().isEmpty()) {
            for (PurchaseItemForm itemForm : form.getItems()) {
                PurchaseItem item = new PurchaseItem();
                item.setPurchaseId(purchase.getId());
                item.setItemName(itemForm.getItemName());
                item.setSpec(itemForm.getSpec());
                item.setQuantity(itemForm.getQuantity());
                item.setUnit(itemForm.getUnit());
                item.setUnitPrice(itemForm.getUnitPrice());
                if (itemForm.getUnitPrice() != null && itemForm.getQuantity() != null) {
                    item.setTotalPrice(itemForm.getUnitPrice().multiply(new BigDecimal(itemForm.getQuantity())));
                }
                purchaseItemMapper.insert(item);
            }
        }

        // 4. 保存审批记录
        saveApprovalRecord(purchase.getId(), "PURCHASE", userId, "SUBMIT", "提交申请");

        // 5. 创建审批流程节点
        approvalNodeService.createNodes("PURCHASE", purchase.getId(), userId);

        log.info("创建采购申请成功: purchaseNo={}, userId={}, amount={}", purchaseNo, userId, form.getBudgetAmount());
        return purchase.getId();
    }

    @Override
    public List<PurchaseVO> listPurchase(Long userId, String status, String purchaseType) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, Purchase::getUserId, userId);
        wrapper.eq(status != null, Purchase::getStatus, status);
        wrapper.eq(purchaseType != null, Purchase::getPurchaseType, purchaseType);
        wrapper.orderByDesc(Purchase::getCreateTime);

        List<Purchase> purchases = purchaseMapper.selectList(wrapper);
        if (purchases.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量预加载关联数据，避免 N+1
        List<Long> purchaseIds = purchases.stream().map(Purchase::getId).collect(Collectors.toList());

        LambdaQueryWrapper<PurchaseItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(PurchaseItem::getPurchaseId, purchaseIds);
        Map<Long, List<PurchaseItem>> itemsMap = purchaseItemMapper.selectList(itemWrapper)
            .stream().collect(Collectors.groupingBy(PurchaseItem::getPurchaseId));

        LambdaQueryWrapper<PurchaseProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.in(PurchaseProgress::getPurchaseId, purchaseIds);
        progressWrapper.orderByAsc(PurchaseProgress::getCreateTime);
        Map<Long, List<PurchaseProgress>> progressMap = progressMapper.selectList(progressWrapper)
            .stream().collect(Collectors.groupingBy(PurchaseProgress::getPurchaseId));

        LambdaQueryWrapper<ApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ApprovalRecord::getBusinessType, "PURCHASE");
        recordWrapper.in(ApprovalRecord::getBusinessId, purchaseIds);
        recordWrapper.orderByAsc(ApprovalRecord::getCreateTime);
        Map<Long, List<ApprovalRecord>> recordMap = approvalRecordMapper.selectList(recordWrapper)
            .stream().collect(Collectors.groupingBy(ApprovalRecord::getBusinessId));

        return purchases.stream()
            .map(p -> convertToVOWithDetails(p, itemsMap.getOrDefault(p.getId(), Collections.emptyList()),
                progressMap.getOrDefault(p.getId(), Collections.emptyList()),
                recordMap.getOrDefault(p.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }

    @Override
    public PurchaseVO getPurchaseById(Long id, Long userId) {
        Purchase purchase = purchaseMapper.selectById(id);
        if (purchase == null) {
            return null;
        }
        // 权限校验：仅申请人本人可查看
        if (!purchase.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该采购申请");
        }
        return convertToVO(purchase);
    }

    @Override
    @Transactional
    public void approvePurchase(Long id, ApproveForm form, Long approverId) {
        // 验证审批类型
        if (!"APPROVE".equals(form.getApproveType()) && !"REJECT".equals(form.getApproveType())) {
            throw new BusinessException(400, "无效的审批类型");
        }

        // [A1 第二步] 委托 UniversalApprovalService 走状态机 + 乐观锁
        ApprovalEvent event = "APPROVE".equals(form.getApproveType()) ? ApprovalEvent.APPROVE : ApprovalEvent.REJECT;
        universalService.fire("PURCHASE", id, approverId, event, form.getComment());

        // 业务副作用：保存审批记录
        saveApprovalRecord(id, "PURCHASE", approverId, form.getApproveType(), form.getComment());

        // 业务副作用：审批通过后改 deliveryStatus = PURCHASING
        if ("APPROVE".equals(form.getApproveType())) {
            Purchase purchase = purchaseMapper.selectById(id);
            if (purchase != null) {
                purchase.setDeliveryStatus("PURCHASING");
                purchase.setUpdateTime(LocalDateTime.now());
                purchaseMapper.updateById(purchase);
            }
        }

        log.info("审批采购申请: id={}, approverId={}, type={}", id, approverId, form.getApproveType());
    }

    @Override
    @Transactional
    public void cancelPurchase(Long id, Long userId) {
        // [A1 第二步] 委托 UniversalApprovalService 走状态机 + 乐观锁
        universalService.fire("PURCHASE", id, userId, ApprovalEvent.WITHDRAW, "申请人撤回");

        // 业务副作用：保存撤回记录
        saveApprovalRecord(id, "PURCHASE", userId, "CANCEL", "撤回申请");
        log.info("撤回采购申请: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void updateProgress(Long id, PurchaseProgressForm form, Long operatorId) {
        Purchase purchase = purchaseMapper.selectById(id);
        if (purchase == null) {
            throw new BusinessException(404, "采购申请不存在");
        }
        // 只有 APPROVED 状态且 deliveryStatus 为 PURCHASING 才能更新进度
        if (!"APPROVED".equals(purchase.getStatus()) || !"PURCHASING".equals(purchase.getDeliveryStatus())) {
            throw new BusinessException(403, "当前状态不允许更新进度");
        }

        // 保存进度记录
        PurchaseProgress progress = new PurchaseProgress();
        progress.setPurchaseId(id);
        progress.setProgressType(form.getProgressType());
        progress.setProgressDesc(form.getProgressDesc());
        progress.setProgressTime(form.getProgressTime());
        progress.setOperatorId(operatorId);
        progress.setCreateTime(java.time.LocalDateTime.now());
        progressMapper.insert(progress);

        // 更新采购单交付状态
        purchase.setDeliveryStatus(form.getProgressType());
        if ("DELIVERED".equals(form.getProgressType()) || "COMPLETED".equals(form.getProgressType())) {
            purchase.setActualDeliveryDate(LocalDate.now());
        }
        if ("COMPLETED".equals(form.getProgressType())) {
            purchase.setStatus("COMPLETED");
        }
        purchase.setUpdateTime(LocalDateTime.now());

        // 使用乐观锁更新
        int rows = purchaseMapper.update(
            purchase,
            new LambdaQueryWrapper<Purchase>()
                .eq(Purchase::getId, id)
                .eq(Purchase::getDeliveryStatus, "PURCHASING")
                .eq(Purchase::getVersion, purchase.getVersion())
        );

        if (rows == 0) {
            throw new BusinessException(400, "数据已被其他操作修改，请刷新后重试");
        }

        log.info("更新采购进度: id={}, type={}", id, form.getProgressType());
    }

    @Override
    public PurchaseStatisticsVO getStatistics(Long deptId, String purchaseType) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deptId != null, Purchase::getDeptId, deptId);
        wrapper.eq(purchaseType != null, Purchase::getPurchaseType, purchaseType);

        List<Purchase> purchases = purchaseMapper.selectList(wrapper);

        PurchaseStatisticsVO vo = new PurchaseStatisticsVO();
        vo.setTotalCount(purchases.size());
        vo.setTotalAmount(purchases.stream()
            .map(Purchase::getBudgetAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setPendingCount((int) purchases.stream().filter(p -> "PENDING".equals(p.getStatus())).count());
        vo.setApprovedCount((int) purchases.stream().filter(p -> "APPROVED".equals(p.getStatus()) || "COMPLETED".equals(p.getStatus())).count());
        vo.setRejectedCount((int) purchases.stream().filter(p -> "REJECTED".equals(p.getStatus())).count());

        Map<String, BigDecimal> typeAmountMap = purchases.stream()
            .filter(p -> p.getBudgetAmount() != null)
            .collect(Collectors.groupingBy(Purchase::getPurchaseType,
                Collectors.reducing(BigDecimal.ZERO, Purchase::getBudgetAmount, BigDecimal::add)));
        vo.setTypeAmountMap(typeAmountMap);

        Map<String, Integer> typeCountMap = purchases.stream()
            .collect(Collectors.groupingBy(Purchase::getPurchaseType, Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().intValue()));
        vo.setTypeCountMap(typeCountMap);

        return vo;
    }

    private String generatePurchaseNo() {
        String prefix = "CG" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用行锁保证并发唯一性
        String maxNo = purchaseMapper.selectMaxPurchaseNoForUpdate(prefix + "%");
        if (maxNo == null) {
            return prefix + "0001";
        }
        int seq = Integer.parseInt(maxNo.substring(maxNo.length() - 4)) + 1;
        return prefix + String.format("%04d", seq);
    }

    private void saveApprovalRecord(Long businessId, String businessType, Long approverId, String approveType, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApproverId(approverId);
        record.setApproveType(approveType);
        record.setComment(comment);
        record.setCreateTime(java.time.LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }

    private PurchaseVO convertToVO(Purchase purchase) {
        PurchaseVO vo = new PurchaseVO();
        vo.setId(purchase.getId());
        vo.setPurchaseNo(purchase.getPurchaseNo());
        vo.setUserId(purchase.getUserId());
        vo.setPurchaseType(purchase.getPurchaseType());
        vo.setPurchaseTypeDesc(PURCHASE_TYPE_MAP.getOrDefault(purchase.getPurchaseType(), purchase.getPurchaseType()));
        vo.setItemName(purchase.getItemName());
        vo.setQuantity(purchase.getQuantity());
        vo.setUnit(purchase.getUnit());
        vo.setBudgetAmount(purchase.getBudgetAmount());
        vo.setSupplierName(purchase.getSupplierName());
        vo.setSupplierContact(purchase.getSupplierContact());
        vo.setSupplierPhone(purchase.getSupplierPhone());
        vo.setReason(purchase.getReason());
        vo.setStatus(purchase.getStatus());
        vo.setStatusDesc(STATUS_MAP.getOrDefault(purchase.getStatus(), purchase.getStatus()));
        vo.setDeliveryStatus(purchase.getDeliveryStatus());
        vo.setDeliveryStatusDesc(DELIVERY_STATUS_MAP.getOrDefault(purchase.getDeliveryStatus(), purchase.getDeliveryStatus()));
        vo.setExpectedDeliveryDate(purchase.getExpectedDeliveryDate());
        vo.setActualDeliveryDate(purchase.getActualDeliveryDate());
        vo.setCreateTime(purchase.getCreateTime());
        vo.setUpdateTime(purchase.getUpdateTime());

        if (purchase.getAttachments() != null) {
            vo.setAttachmentUrls(Arrays.asList(purchase.getAttachments().split(",")));
        }

        // 获取采购明细
        LambdaQueryWrapper<PurchaseItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PurchaseItem::getPurchaseId, purchase.getId());
        List<PurchaseItem> items = purchaseItemMapper.selectList(itemWrapper);
        vo.setItems(items.stream().map(i -> {
            PurchaseItemVO itemVO = new PurchaseItemVO();
            itemVO.setId(i.getId());
            itemVO.setItemName(i.getItemName());
            itemVO.setSpec(i.getSpec());
            itemVO.setQuantity(i.getQuantity());
            itemVO.setUnit(i.getUnit());
            itemVO.setUnitPrice(i.getUnitPrice());
            itemVO.setTotalPrice(i.getTotalPrice());
            itemVO.setRemark(i.getRemark());
            return itemVO;
        }).collect(Collectors.toList()));

        // 获取进度记录
        LambdaQueryWrapper<PurchaseProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(PurchaseProgress::getPurchaseId, purchase.getId());
        progressWrapper.orderByAsc(PurchaseProgress::getCreateTime);
        List<PurchaseProgress> progressList = progressMapper.selectList(progressWrapper);
        vo.setProgressRecords(progressList.stream().map(p -> {
            PurchaseProgressVO pVO = new PurchaseProgressVO();
            pVO.setId(p.getId());
            pVO.setProgressType(p.getProgressType());
            pVO.setProgressDesc(p.getProgressDesc());
            pVO.setProgressTime(p.getProgressTime());
            pVO.setOperatorId(p.getOperatorId());
            return pVO;
        }).collect(Collectors.toList()));

        // 获取审批历史
        LambdaQueryWrapper<ApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ApprovalRecord::getBusinessType, "PURCHASE");
        recordWrapper.eq(ApprovalRecord::getBusinessId, purchase.getId());
        recordWrapper.orderByAsc(ApprovalRecord::getCreateTime);
        List<ApprovalRecord> records = approvalRecordMapper.selectList(recordWrapper);
        vo.setApprovalRecords(records.stream().map(r -> {
            ApprovalRecordVO ar = new ApprovalRecordVO();
            ar.setId(r.getId());
            ar.setApproveType(r.getApproveType());
            ar.setApproveTypeDesc("APPROVE".equals(r.getApproveType()) ? "同意" : "REJECT".equals(r.getApproveType()) ? "拒绝" : r.getApproveType());
            ar.setComment(r.getComment());
            ar.setCreateTime(r.getCreateTime());
            return ar;
        }).collect(Collectors.toList()));

        return vo;
    }

    private PurchaseVO convertToVOWithDetails(Purchase purchase, List<PurchaseItem> items,
                                               List<PurchaseProgress> progressList, List<ApprovalRecord> records) {
        PurchaseVO vo = new PurchaseVO();
        vo.setId(purchase.getId());
        vo.setPurchaseNo(purchase.getPurchaseNo());
        vo.setUserId(purchase.getUserId());
        vo.setPurchaseType(purchase.getPurchaseType());
        vo.setPurchaseTypeDesc(PURCHASE_TYPE_MAP.getOrDefault(purchase.getPurchaseType(), purchase.getPurchaseType()));
        vo.setItemName(purchase.getItemName());
        vo.setQuantity(purchase.getQuantity());
        vo.setUnit(purchase.getUnit());
        vo.setBudgetAmount(purchase.getBudgetAmount());
        vo.setSupplierName(purchase.getSupplierName());
        vo.setSupplierContact(purchase.getSupplierContact());
        vo.setSupplierPhone(purchase.getSupplierPhone());
        vo.setReason(purchase.getReason());
        vo.setStatus(purchase.getStatus());
        vo.setStatusDesc(STATUS_MAP.getOrDefault(purchase.getStatus(), purchase.getStatus()));
        vo.setDeliveryStatus(purchase.getDeliveryStatus());
        vo.setDeliveryStatusDesc(DELIVERY_STATUS_MAP.getOrDefault(purchase.getDeliveryStatus(), purchase.getDeliveryStatus()));
        vo.setExpectedDeliveryDate(purchase.getExpectedDeliveryDate());
        vo.setActualDeliveryDate(purchase.getActualDeliveryDate());
        vo.setCreateTime(purchase.getCreateTime());
        vo.setUpdateTime(purchase.getUpdateTime());

        if (purchase.getAttachments() != null) {
            vo.setAttachmentUrls(Arrays.asList(purchase.getAttachments().split(",")));
        }

        vo.setItems(items.stream().map(i -> {
            PurchaseItemVO itemVO = new PurchaseItemVO();
            itemVO.setId(i.getId());
            itemVO.setItemName(i.getItemName());
            itemVO.setSpec(i.getSpec());
            itemVO.setQuantity(i.getQuantity());
            itemVO.setUnit(i.getUnit());
            itemVO.setUnitPrice(i.getUnitPrice());
            itemVO.setTotalPrice(i.getTotalPrice());
            itemVO.setRemark(i.getRemark());
            return itemVO;
        }).collect(Collectors.toList()));

        vo.setProgressRecords(progressList.stream().map(p -> {
            PurchaseProgressVO pVO = new PurchaseProgressVO();
            pVO.setId(p.getId());
            pVO.setProgressType(p.getProgressType());
            pVO.setProgressDesc(p.getProgressDesc());
            pVO.setProgressTime(p.getProgressTime());
            pVO.setOperatorId(p.getOperatorId());
            return pVO;
        }).collect(Collectors.toList()));

        vo.setApprovalRecords(records.stream().map(r -> {
            ApprovalRecordVO ar = new ApprovalRecordVO();
            ar.setId(r.getId());
            ar.setApproveType(r.getApproveType());
            ar.setApproveTypeDesc("APPROVE".equals(r.getApproveType()) ? "同意" : "REJECT".equals(r.getApproveType()) ? "拒绝" : r.getApproveType());
            ar.setComment(r.getComment());
            ar.setCreateTime(r.getCreateTime());
            return ar;
        }).collect(Collectors.toList()));

        return vo;
    }
}