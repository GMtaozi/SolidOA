package com.solidoa.collaboration.dto;

import lombok.Data;

@Data
public class MessageDTO {
    private Long id;
    private String msgNo;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private String senderName;
    private Long receiverId;
    private Integer isRead;
    private String relatedType;
    private Long relatedId;
    private String createTime;
}