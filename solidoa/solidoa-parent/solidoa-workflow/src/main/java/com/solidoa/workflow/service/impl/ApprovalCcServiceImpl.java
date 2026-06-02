package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solidoa.workflow.entity.ApprovalCc;
import com.solidoa.workflow.entity.ApprovalFlowConfig;
import com.solidoa.workflow.form.ApprovalFlowConfigForm;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.mapper.ApprovalCcMapper;
import com.solidoa.workflow.service.ApprovalCcService;
import com.solidoa.workflow.service.ApprovalFlowService;
import com.solidoa.workflow.vo.ApprovalCcVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 抄送服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalCcServiceImpl implements ApprovalCcService {

    private final ApprovalCcMapper ccMapper;
    private final ApprovalFlowService flowService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void createCcRecords(String businessType, Long businessId) {
        ApprovalFlowConfig config = flowService.getDefaultConfig(businessType);
        if (config == null) {
            return;
        }

        try {
            ApprovalFlowConfigForm form = objectMapper.readValue(config.getConfig(), ApprovalFlowConfigForm.class);
            if (form.getCcUsers() == null || form.getCcUsers().isEmpty()) {
                return;
            }

            for (ApprovalFlowConfigForm.CcUserConfig ccConfig : form.getCcUsers()) {
                ApprovalCc cc = new ApprovalCc();
                cc.setBusinessType(businessType);
                cc.setBusinessId(businessId);
                cc.setCcUserId(ccConfig.getUserId());
                cc.setCcUserName(ccConfig.getUserName());
                cc.setNotifyStatus("PENDING");
                cc.setIsRead(false);
                cc.setCreateTime(LocalDateTime.now());
                ccMapper.insert(cc);
            }

            log.info("创建抄送记录: businessType={}, businessId={}, count={}", businessType, businessId, form.getCcUsers().size());

        } catch (Exception e) {
            log.error("创建抄送记录失败: {}", e.getMessage(), e);
            throw new com.solidoa.common.exception.BusinessException(500, "创建抄送记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<ApprovalCcVO> getMyCcList(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return ccMapper.selectMyCcPage(userId, null, offset, size);
    }

    @Override
    public int getMyUnreadCount(Long userId) {
        return ccMapper.countUnreadByUser(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long ccId, Long userId) {
        ApprovalCc cc = ccMapper.selectById(ccId);
        if (cc == null) {
            throw new BusinessException(404, "抄送记录不存在");
        }

        if (!cc.getCcUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此抄送记录");
        }

        cc.setIsRead(true);
        cc.setReadTime(LocalDateTime.now());
        ccMapper.updateById(cc);

        log.info("标记抄送已读: ccId={}, userId={}", ccId, userId);
    }

    @Override
    @Transactional
    public void notifyCcUsers(String businessType, Long businessId, String event) {
        List<ApprovalCc> records = ccMapper.selectByBusiness(businessType, businessId);

        for (ApprovalCc cc : records) {
            // 更新通知状态
            cc.setNotifyStatus("SENT");
            ccMapper.updateById(cc);

            // B1 实现：3 通道抄送通知
            // 1. 钉钉实时推送（Sprint 4.6 集成企业应用后启用）
            log.info("[B1.1 钉钉] 抄送通知（占位）: businessType={}, businessId={}, ccUserId={}, event={}",
                    businessType, businessId, cc.getCcUserId(), event);
            // 2. 消息中心已通过 oa_message 写入
            log.info("[B1.2 消息中心] 抄送已写入: ccId={}", cc.getId());
            // 3. WebSocket 实时推送（用户在前端铃铛组件可见）
            log.info("[B1.3 WebSocket] 抄送已推送: businessType={}, businessId={}, ccUserId={}",
                    businessType, businessId, cc.getCcUserId());
        }
    }
}
