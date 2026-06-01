package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 采购进度VO
 */
@Data
public class PurchaseProgressVO {

    private Long id;
    private String progressType;
    private String progressTypeDesc;
    private String progressDesc;
    private LocalDateTime progressTime;
    private Long operatorId;
    private String operatorName;
}