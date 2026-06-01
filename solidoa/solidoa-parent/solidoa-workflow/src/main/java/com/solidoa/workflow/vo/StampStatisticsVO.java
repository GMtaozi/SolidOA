package com.solidoa.workflow.vo;

import lombok.Data;
import java.util.Map;

/**
 * 用印统计VO
 */
@Data
public class StampStatisticsVO {

    private Integer totalCount;
    private Integer pendingCount;
    private Integer approvedCount;
    private Integer rejectedCount;
    private Map<String, Integer> typeCountMap;
}