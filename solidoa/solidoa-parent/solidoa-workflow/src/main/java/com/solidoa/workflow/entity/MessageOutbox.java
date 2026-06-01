package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息发送记录表（本地消息表）
 * 用于 MQ 消息发送失败后的补偿
 */
@Data
@TableName("wf_message_outbox")
public class MessageOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息唯一标识（用于幂等）
     */
    private String messageId;

    /**
     * 消息类型：APPROVAL / REMINDER / NOTIFICATION
     */
    private String messageType;

    /**
     * 业务类型：LEAVE / EXPENSE / REPAIR 等
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 消息内容（JSON）
     */
    private String messageContent;

    /**
     * 发送状态：PENDING / SENDING / SENT / FAILED
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 最后错误信息
     */
    private String lastError;

    /**
     * 下次发送时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}