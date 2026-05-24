package com.solidoa.common.vo;

import lombok.Data;

@Data
public class MessageDTO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long senderId;
    private Long receiverId;
    private String relatedType;
    private Long relatedId;
}