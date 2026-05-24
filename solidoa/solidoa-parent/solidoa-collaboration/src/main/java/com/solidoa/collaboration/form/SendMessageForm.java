package com.solidoa.collaboration.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageForm {
    @NotNull(message = "接收人不能为空")
    private Long receiverId;

    private String title;
    private String content;
    private String type;
    private Integer priority;
    private String relatedType;
    private Long relatedId;
}