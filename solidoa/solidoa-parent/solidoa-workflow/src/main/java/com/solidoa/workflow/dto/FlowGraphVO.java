package com.solidoa.workflow.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程图 VO（前端可视化用）
 * 含 nodes + edges，前端可直接画有向无环图
 */
@Data
public class FlowGraphVO {
    /** 业务类型 */
    private String businessType;
    /** 业务单号 */
    private String businessNo;
    /** 业务 ID */
    private Long businessId;
    /** 申请人 */
    private String applicantName;
    /** 当前状态 */
    private String currentState;
    /** 当前节点 order（-1 表示终态） */
    private Integer currentNodeOrder;
    /** 节点列表 */
    private List<GraphNode> nodes;
    /** 边列表 */
    private List<GraphEdge> edges;
    /** 抄送人列表 */
    private List<GraphNode> ccUsers;
    /** 总节点数 */
    private Integer totalNodes;
    /** 已完成节点数 */
    private Integer completedNodes;
}
