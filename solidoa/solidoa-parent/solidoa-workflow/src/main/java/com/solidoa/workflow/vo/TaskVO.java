package com.solidoa.workflow.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskVO {
    private String taskId;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private String title;
    private String content;
    private Long applyUserId;
    private String applyUserName;
    private LocalDateTime applyTime;
    private String status;
}