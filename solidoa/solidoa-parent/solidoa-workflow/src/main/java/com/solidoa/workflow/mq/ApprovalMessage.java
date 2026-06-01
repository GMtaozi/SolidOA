package com.solidoa.workflow.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息版本号（用于消息格式升级兼容）
     * 格式：主版本.次版本（如 1.0, 2.0）
     */
    private String version = "1.0";

    /**
     * 消息类型：APPROVAL, REMINDER, CANCEL
     */
    private String messageType;

    private String businessType;  // LEAVE, EXPENSE
    private Long businessId;
    private Long approverId;
    private String approverName;
    private String actionType;    // APPROVE, REJECT, TRANSFER, ADD_SIGN
    private String comment;
    private Long applyUserId;
    private String applyUserName;
    private String businessNo;
    private LocalDateTime createTime;
}