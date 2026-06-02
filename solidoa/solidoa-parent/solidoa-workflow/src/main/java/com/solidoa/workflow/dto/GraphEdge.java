package com.solidoa.workflow.dto;

import lombok.Data;

/**
 * 流程图边（节点连线）
 * type:
 *   - SEQUENCE: 顺序流（默认，按 order 串联）
 *   - PARALLEL: 并行分支
 *   - CONDITION: 条件分支
 */
@Data
public class GraphEdge {
    private Long fromId;
    private Long toId;
    private String fromOrder;
    private String toOrder;
    private String type;
    private String label;
}
