package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.workflow.dto.FlowGraphVO;
import com.solidoa.workflow.dto.GraphEdge;
import com.solidoa.workflow.dto.GraphNode;
import com.solidoa.workflow.entity.ApprovalCc;
import com.solidoa.workflow.entity.ApprovalNode;
import com.solidoa.workflow.mapper.ApprovalCcMapper;
import com.solidoa.workflow.mapper.ApprovalNodeMapper;
import com.solidoa.workflow.service.FlowGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlowGraphServiceImpl implements FlowGraphService {

    private final ApprovalNodeMapper nodeMapper;
    private final ApprovalCcMapper ccMapper;

    @Override
    public FlowGraphVO getFlowGraph(String businessType, Long businessId) {
        FlowGraphVO vo = new FlowGraphVO();
        vo.setBusinessType(businessType);
        vo.setBusinessId(businessId);

        // 1. 节点列表
        LambdaQueryWrapper<ApprovalNode> nodeQuery = new LambdaQueryWrapper<>();
        nodeQuery.eq(ApprovalNode::getBusinessType, businessType)
                .eq(ApprovalNode::getBusinessId, businessId)
                .orderByAsc(ApprovalNode::getNodeOrder);
        List<ApprovalNode> dbNodes = nodeMapper.selectList(nodeQuery);

        List<GraphNode> nodes = new ArrayList<>();
        int completed = 0;
        int currentOrder = -1;

        for (ApprovalNode n : dbNodes) {
            GraphNode g = new GraphNode();
            g.setId(n.getId());
            g.setOrder(n.getNodeOrder());
            g.setName("节点 " + n.getNodeOrder());
            g.setType(n.getNodeType());
            g.setMode(n.getNodeMode());
            g.setApproverId(n.getApproverId());
            g.setApproverName(n.getApproverName());
            g.setStatus(n.getStatus());
            g.setApprovedTime(n.getApprovedTime() != null ? n.getApprovedTime().toString() : null);
            g.setComment(n.getComment());
            nodes.add(g);

            if ("APPROVED".equals(n.getStatus())) completed++;
            if ("PENDING".equals(n.getStatus()) && currentOrder == -1) {
                currentOrder = n.getNodeOrder();
            }
        }

        vo.setNodes(nodes);
        vo.setTotalNodes(nodes.size());
        vo.setCompletedNodes(completed);
        vo.setCurrentNodeOrder(currentOrder);

        // 2. 抄送人
        LambdaQueryWrapper<ApprovalCc> ccQuery = new LambdaQueryWrapper<>();
        ccQuery.eq(ApprovalCc::getBusinessType, businessType)
                .eq(ApprovalCc::getBusinessId, businessId);
        List<ApprovalCc> dbCcs = ccMapper.selectList(ccQuery);
        List<GraphNode> ccNodes = new ArrayList<>();
        for (ApprovalCc cc : dbCcs) {
            GraphNode g = new GraphNode();
            g.setId(cc.getId());
            g.setName(cc.getCcUserName());
            g.setType("CC");
            g.setApproverId(cc.getCcUserId());
            g.setApproverName(cc.getCcUserName());
            g.setStatus(Boolean.TRUE.equals(cc.getIsRead()) ? "READ" : "UNREAD");
            ccNodes.add(g);
        }
        vo.setCcUsers(ccNodes);

        // 3. 推断 edges（兜底）
        vo.setEdges(deriveEdges(nodes));

        return vo;
    }

    @Override
    public List<GraphEdge> deriveEdges(List<GraphNode> nodes) {
        if (nodes == null || nodes.size() < 2) return List.of();

        // 按 parentNodeId 分组：同一层级的节点按 order 串联
        // 简化策略：忽略父节点分组，按 order 顺序串联
        // 复杂场景（加签嵌套）后续可优化
        List<GraphEdge> edges = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            GraphNode from = nodes.get(i);
            GraphNode to = nodes.get(i + 1);
            // 跳过抄送节点：type=CC 的节点不参与主流程连线
            if ("CC".equals(from.getType()) || "CC".equals(to.getType())) continue;

            GraphEdge edge = new GraphEdge();
            edge.setFromId(from.getId());
            edge.setToId(to.getId());
            edge.setFromOrder(String.valueOf(from.getOrder()));
            edge.setToOrder(String.valueOf(to.getOrder()));
            edge.setType("SEQUENCE");
            edges.add(edge);
        }
        return edges;
    }
}
