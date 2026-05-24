package com.solidoa.collaboration.service;

import com.solidoa.collaboration.form.SendMessageForm;
import com.solidoa.collaboration.dto.MessageDTO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;

public interface MessageService {
    Long send(SendMessageForm form, Long senderId);
    PageVO<MessageDTO> pageList(PageDTO dto, Long userId);
    long getUnreadCount(Long userId);
    void markRead(Long id);
    void markAllRead(Long userId);
    void sendToUser(Long userId, String type, String title, String content);
}