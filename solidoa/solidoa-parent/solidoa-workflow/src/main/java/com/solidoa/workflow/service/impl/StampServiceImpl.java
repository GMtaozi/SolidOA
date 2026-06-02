package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.workflow.entity.*;
import com.solidoa.workflow.form.*;
import com.solidoa.workflow.mapper.*;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.util.CryptoUtil;
import com.solidoa.workflow.service.StampService;
import com.solidoa.workflow.service.ApprovalNodeService;
import com.solidoa.workflow.service.UniversalApprovalService;
import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 用印申请服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StampServiceImpl implements StampService {

    private final StampMapper stampMapper;
    private final StampRecordMapper stampRecordMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalNodeService approvalNodeService;
    private final UniversalApprovalService universalService;

    /** 用印类型映射 */
    private static final Map<String, String> STAMP_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> STATUS_MAP = new HashMap<>();

    static {
        STAMP_TYPE_MAP.put("PUBLIC", "公章");
        STAMP_TYPE_MAP.put("CONTRACT", "合同章");
        STAMP_TYPE_MAP.put("LEGAL", "法人章");
        STAMP_TYPE_MAP.put("DEPT", "部门章");

        STATUS_MAP.put("PENDING", "审批中");
        STATUS_MAP.put("APPROVED", "已同意");
        STATUS_MAP.put("REJECTED", "已拒绝");
        STATUS_MAP.put("COMPLETED", "已完成");
        STATUS_MAP.put("CANCELLED", "已撤回");
    }

    @Override
    @Transactional
    public Long createStamp(StampForm form, Long userId) {
        // 1. 生成用印单号 YS + 年月日 + 序号
        String stampNo = generateStampNo();

        // 2. 创建用印申请
        Stamp stamp = new Stamp();
        stamp.setStampNo(stampNo);
        stamp.setUserId(userId);
        stamp.setDeptId(form.getDeptId());
        stamp.setStampType(form.getStampType());
        stamp.setDocumentName(form.getDocumentName());
        stamp.setDocumentCount(form.getDocumentCount());
        stamp.setStampUsage(form.getUsage());
        stamp.setRemark(form.getRemark());
        stamp.setAttachments(form.getAttachments() != null ? String.join(",", form.getAttachments()) : null);
        stamp.setStatus("PENDING");
        stamp.setCreateTime(LocalDateTime.now());
        stamp.setUpdateTime(LocalDateTime.now());

        stampMapper.insert(stamp);

        // 3. 保存审批记录
        saveApprovalRecord(stamp.getId(), "STAMP", userId, "SUBMIT", "提交申请");

        // 4. 创建审批流程节点
        approvalNodeService.createNodes("STAMP", stamp.getId(), userId);

        log.info("创建用印申请成功: stampNo={}, userId={}", stampNo, userId);
        return stamp.getId();
    }

    @Override
    public List<StampVO> listStamp(Long userId, String status) {
        LambdaQueryWrapper<Stamp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, Stamp::getUserId, userId);
        wrapper.eq(status != null, Stamp::getStatus, status);
        wrapper.orderByDesc(Stamp::getCreateTime);

        List<Stamp> stamps = stampMapper.selectList(wrapper);
        return stamps.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public StampVO getStampById(Long id, Long userId) {
        Stamp stamp = stampMapper.selectById(id);
        if (stamp == null) {
            return null;
        }
        return convertToVO(stamp);
    }

    @Override
    @Transactional
    public void approveStamp(Long id, ApproveForm form, Long approverId) {
        // 验证审批类型
        if (!"APPROVE".equals(form.getApproveType()) && !"REJECT".equals(form.getApproveType())) {
            throw new BusinessException(400, "无效的审批类型");
        }
        // [A1 第二步] 委托 UniversalApprovalService 走状态机 + 乐观锁
        ApprovalEvent event = "APPROVE".equals(form.getApproveType()) ? ApprovalEvent.APPROVE : ApprovalEvent.REJECT;
        universalService.fire("STAMP", id, approverId, event, form.getComment());

        // 业务副作用：保存审批记录
        saveApprovalRecord(id, "STAMP", approverId, form.getApproveType(), form.getComment());

        log.info("审批用印申请: id={}, approverId={}, type={}", id, approverId, form.getApproveType());
    }

    @Override
    @Transactional
    public void cancelStamp(Long id, Long userId) {
        // [A1 第二步] 委托 UniversalApprovalService 走状态机 + 乐观锁
        universalService.fire("STAMP", id, userId, ApprovalEvent.WITHDRAW, "申请人撤回");

        // 业务副作用：保存撤回记录
        saveApprovalRecord(id, "STAMP", userId, "CANCEL", "撤回申请");
        log.info("撤回用印申请: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void recordStamp(Long id, StampRecordForm form, Long operatorId) {
        Stamp stamp = stampMapper.selectById(id);
        if (stamp == null) {
            throw new BusinessException(404, "用印申请不存在");
        }
        if (!"APPROVED".equals(stamp.getStatus())) {
            throw new BusinessException(400, "仅已审批通过的申请可以登记用印");
        }

        // 创建用印记录
        StampRecord record = new StampRecord();
        record.setStampId(id);
        record.setStampTime(form.getStampTime());
        record.setReceivedBy(form.getReceivedBy());
        // 手机号加密存储
        record.setReceivedMobile(CryptoUtil.encrypt(form.getReceivedMobile()));
        record.setActualCount(form.getActualCount());
        record.setOperatorId(operatorId);
        record.setCreateTime(LocalDateTime.now());
        stampRecordMapper.insert(record);

        // 更新申请状态
        stamp.setStampTime(form.getStampTime());
        stamp.setReceivedBy(form.getReceivedBy());
        stamp.setStatus("COMPLETED");
        stamp.setUpdateTime(LocalDateTime.now());

        // 使用乐观锁更新
        int rows = stampMapper.update(
            stamp,
            new LambdaQueryWrapper<Stamp>()
                .eq(Stamp::getId, id)
                .eq(Stamp::getStatus, "APPROVED")
                .eq(Stamp::getVersion, stamp.getVersion())
        );

        if (rows == 0) {
            throw new BusinessException(400, "数据已被其他操作修改，请刷新后重试");
        }

        log.info("登记物理用印: id={}, receivedBy={}", id, form.getReceivedBy());
    }

    @Override
    public StampStatisticsVO getStatistics(Long deptId) {
        LambdaQueryWrapper<Stamp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deptId != null, Stamp::getDeptId, deptId);

        List<Stamp> stamps = stampMapper.selectList(wrapper);

        StampStatisticsVO vo = new StampStatisticsVO();
        vo.setTotalCount(stamps.size());
        vo.setPendingCount((int) stamps.stream().filter(s -> "PENDING".equals(s.getStatus())).count());
        vo.setApprovedCount((int) stamps.stream().filter(s -> "APPROVED".equals(s.getStatus()) || "COMPLETED".equals(s.getStatus())).count());
        vo.setRejectedCount((int) stamps.stream().filter(s -> "REJECTED".equals(s.getStatus())).count());

        Map<String, Integer> typeCountMap = stamps.stream()
            .collect(Collectors.groupingBy(Stamp::getStampType, Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().intValue()));
        vo.setTypeCountMap(typeCountMap);

        return vo;
    }

    private String generateStampNo() {
        String prefix = "YS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用 FOR UPDATE 行锁，防止并发场景下多个请求获取相同最大单号导致重复
        String maxNo = stampMapper.selectMaxStampNoForUpdate(prefix + "%");
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
        record.setCreateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }

    private StampVO convertToVO(Stamp stamp) {
        StampVO vo = new StampVO();
        vo.setId(stamp.getId());
        vo.setStampNo(stamp.getStampNo());
        vo.setUserId(stamp.getUserId());
        vo.setDeptId(stamp.getDeptId());
        vo.setStampType(stamp.getStampType());
        vo.setStampTypeDesc(STAMP_TYPE_MAP.getOrDefault(stamp.getStampType(), stamp.getStampType()));
        vo.setDocumentName(stamp.getDocumentName());
        vo.setDocumentCount(stamp.getDocumentCount());
        vo.setStampUsage(stamp.getStampUsage());
        vo.setRemark(stamp.getRemark());
        vo.setStatus(stamp.getStatus());
        vo.setStatusDesc(STATUS_MAP.getOrDefault(stamp.getStatus(), stamp.getStatus()));
        vo.setStampTime(stamp.getStampTime());
        vo.setReceivedBy(stamp.getReceivedBy());
        vo.setReceivedMobile(null);
        vo.setCreateTime(stamp.getCreateTime());
        vo.setUpdateTime(stamp.getUpdateTime());

        if (stamp.getAttachments() != null) {
            vo.setAttachmentUrls(Arrays.asList(stamp.getAttachments().split(",")));
        }

        // 获取审批历史
        try {
            LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalRecord::getBusinessType, "STAMP");
            wrapper.eq(ApprovalRecord::getBusinessId, stamp.getId());
            wrapper.orderByAsc(ApprovalRecord::getCreateTime);
            List<ApprovalRecord> records = approvalRecordMapper.selectList(wrapper);
            vo.setApprovalRecords(records.stream().map(r -> {
                ApprovalRecordVO ar = new ApprovalRecordVO();
                ar.setId(r.getId());
                ar.setApproveType(r.getApproveType());
                ar.setApproveTypeDesc("APPROVE".equals(r.getApproveType()) ? "同意" : "REJECT".equals(r.getApproveType()) ? "拒绝" : r.getApproveType());
                ar.setComment(r.getComment());
                ar.setCreateTime(r.getCreateTime());
                return ar;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("获取审批历史失败: {}", e.getMessage());
            vo.setApprovalRecords(Collections.emptyList());
        }

        return vo;
    }
}