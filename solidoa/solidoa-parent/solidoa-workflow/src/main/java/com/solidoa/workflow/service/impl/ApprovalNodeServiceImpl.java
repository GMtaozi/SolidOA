package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solidoa.workflow.entity.ApprovalFlowConfig;
import com.solidoa.workflow.entity.ApprovalNode;
import com.solidoa.workflow.form.ApprovalFlowConfigForm;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.mapper.ApprovalNodeMapper;
import com.solidoa.workflow.service.ApprovalFlowService;
import com.solidoa.workflow.service.ApprovalNodeService;
import com.solidoa.workflow.service.ApprovalCcService;
import com.solidoa.workflow.vo.ApprovalFlowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批节点服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalNodeServiceImpl implements ApprovalNodeService {

    private final ApprovalNodeMapper nodeMapper;
    private final ApprovalFlowService flowService;
    private final ApprovalCcService ccService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void createNodes(String businessType, Long businessId, Long applicantId) {
        // 获取默认审批流程配置
        ApprovalFlowConfig config = flowService.getDefaultConfig(businessType);
        if (config == null) {
            log.warn("未找到审批流程配置: businessType={}", businessType);
            return;
        }

        try {
            ApprovalFlowConfigForm form = objectMapper.readValue(config.getConfig(), ApprovalFlowConfigForm.class);

            // 创建审批节点
            List<ApprovalFlowConfigForm.ApprovalNodeConfig> nodes = form.getNodes();
            if (nodes == null || nodes.isEmpty()) {
                log.warn("审批流程配置无节点: businessType={}", businessType);
                return;
            }
            for (ApprovalFlowConfigForm.ApprovalNodeConfig nodeConfig : nodes) {
                ApprovalNode node = new ApprovalNode();
                node.setBusinessType(businessType);
                node.setBusinessId(businessId);
                node.setNodeOrder(nodeConfig.getOrder());
                node.setNodeType("APPROVER");
                node.setNodeMode(nodeConfig.getMode());
                node.setApproverId(nodeConfig.getApproverId());
                node.setApproverName(nodeConfig.getApproverName());
                node.setStatus("PENDING");
                node.setCreateTime(LocalDateTime.now());
                nodeMapper.insert(node);
            }

            // 创建抄送记录
            if (form.getCcUsers() != null && !form.getCcUsers().isEmpty()) {
                ccService.createCcRecords(businessType, businessId);
            }

            log.info("创建审批节点: businessType={}, businessId={}, nodeCount={}", businessType, businessId, form.getNodes().size());

        } catch (Exception e) {
            log.error("解析审批流程配置失败: {}", e.getMessage(), e);
            throw new BusinessException(400, "解析审批流程配置失败");
        }
    }

    @Override
    public ApprovalFlowVO getApprovalFlow(String businessType, Long businessId) {
        ApprovalFlowVO vo = new ApprovalFlowVO();
        vo.setBusinessType(businessType);
        vo.setBusinessId(businessId);

        List<ApprovalNode> nodes = nodeMapper.selectByBusiness(businessType, businessId);

        vo.setTotalNodes(nodes.size());
        vo.setCompletedNodes((int) nodes.stream().filter(n -> !"PENDING".equals(n.getStatus())).count());

        // 当前节点
        ApprovalNode currentNode = nodeMapper.selectCurrentNode(businessType, businessId);
        if (currentNode != null) {
            ApprovalFlowVO.CurrentNodeVO current = new ApprovalFlowVO.CurrentNodeVO();
            current.setOrder(currentNode.getNodeOrder());
            current.setName(currentNode.getApproverName());
            current.setMode(currentNode.getNodeMode());
            vo.setCurrentNode(current);
        }

        // 节点列表
        List<ApprovalFlowVO.NodeVO> nodeVOs = nodes.stream().map(n -> {
            ApprovalFlowVO.NodeVO nodeVO = new ApprovalFlowVO.NodeVO();
            nodeVO.setOrder(n.getNodeOrder());
            nodeVO.setName(n.getApproverName());
            nodeVO.setNodeType(n.getNodeType());
            nodeVO.setMode(n.getNodeMode());
            nodeVO.setStatus(n.getStatus());
            nodeVO.setApprovedTime(n.getApprovedTime());
            nodeVO.setComment(n.getComment());
            return nodeVO;
        }).collect(Collectors.toList());
        vo.setNodes(nodeVOs);

        return vo;
    }

    @Override
    @Transactional
    public void approve(String businessType, Long businessId, Long approverId, String approveType, String comment) {
        ApprovalNode currentNode = nodeMapper.selectCurrentNode(businessType, businessId);
        if (currentNode == null) {
            throw new BusinessException(400, "没有待审批的节点");
        }

        if (!currentNode.getApproverId().equals(approverId)) {
            throw new BusinessException(403, "当前审批人不匹配，无权审批此节点");
        }

        // 使用乐观锁：带条件更新，确保节点状态仍为 PENDING
        LambdaUpdateWrapper<ApprovalNode> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApprovalNode::getId, currentNode.getId())
               .eq(ApprovalNode::getStatus, "PENDING")
               .set(ApprovalNode::getStatus, approveType)
               .set(ApprovalNode::getApprovedTime, LocalDateTime.now())
               .set(ApprovalNode::getComment, comment);
        int updated = nodeMapper.update(null, wrapper);
        if (updated == 0) {
            throw new BusinessException(409, "节点状态已变更，请刷新后重试");
        }

        // 如果是拒绝，直接结束
        if ("REJECTED".equals(approveType)) {
            log.info("审批拒绝: businessType={}, businessId={}, approverId={}", businessType, businessId, approverId);
            return;
        }

        // 如果是会签(ALL)模式，检查同节点是否还有人未审批
        if ("ALL".equals(currentNode.getNodeMode())) {
            int pendingCount = nodeMapper.countPendingInNode(businessType, businessId, currentNode.getNodeOrder());
            if (pendingCount > 0) {
                log.info("会签节点还有{}人未审批", pendingCount);
                return;
            }
        }

        // 通知抄送人（实时抄送）
        ccService.notifyCcUsers(businessType, businessId, "APPROVE");

        log.info("审批节点完成: businessType={}, businessId={}, nodeOrder={}", businessType, businessId, currentNode.getNodeOrder());
    }

    @Override
    public int countMyPending(Long userId) {
        return nodeMapper.countPendingByApprover(userId);
    }

    @Override
    public ApprovalFlowVO.CurrentNodeVO getCurrentNode(String businessType, Long businessId) {
        ApprovalNode currentNode = nodeMapper.selectCurrentNode(businessType, businessId);
        if (currentNode == null) {
            return null;
        }

        ApprovalFlowVO.CurrentNodeVO vo = new ApprovalFlowVO.CurrentNodeVO();
        vo.setOrder(currentNode.getNodeOrder());
        vo.setName(currentNode.getApproverName());
        vo.setMode(currentNode.getNodeMode());
        return vo;
    }
}
