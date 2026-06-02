package com.solidoa.workflow.service;

import com.solidoa.workflow.dto.FlowGraphVO;
import com.solidoa.workflow.dto.GraphEdge;
import com.solidoa.workflow.dto.GraphNode;

/**
 * 流程图服务接口
 * 提供 nodes + edges 结构供前端可视化
 */
public interface FlowGraphService {

    /**
     * 获取完整流程图（含 nodes/edges/ccUsers）
     * @param businessType 业务类型（LEAVE/EXPENSE/STAMP/PURCHASE）
     * @param businessId 业务单据 ID
     * @return 流程图数据
     */
    FlowGraphVO getFlowGraph(String businessType, Long businessId);

    /**
     * 推断 edges（兜底逻辑）
     * 当 oa_approval_node 没有保存 edge 信息时，按 nodeOrder 自动推 SEQUENCE 边
     */
    java.util.List<GraphEdge> deriveEdges(java.util.List<GraphNode> nodes);
}
