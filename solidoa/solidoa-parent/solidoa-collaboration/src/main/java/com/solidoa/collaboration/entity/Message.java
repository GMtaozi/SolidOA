package com.solidoa.collaboration.entity;

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
    private Integer priority;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private Integer isRead;
    private LocalDateTime readTime;
    private String relatedType;
    private Long relatedId;
    private LocalDateTime createTime;
}