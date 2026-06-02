package com.solidoa.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String msgNo;

    private String title;

    private String content;

    private String type;

    private Integer priority = 0;

    private Long senderId;

    private String senderName;

    private Long receiverId;

    private Integer isRead = 0;

    private LocalDateTime readTime;

    private String relatedType;

    private Long relatedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
