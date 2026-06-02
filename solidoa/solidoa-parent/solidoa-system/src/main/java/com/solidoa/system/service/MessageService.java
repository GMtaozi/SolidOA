package com.solidoa.system.service;

import com.solidoa.common.vo.MessageDTO;

public interface MessageService {
    /**
     * 发送消息
     */
    void sendMessage(MessageDTO message);

    /**
     * 获取用户未读消息数
     */
    Integer getUnreadCount(Long userId);

    /**
     * 标记已读
     */
    void markAsRead(Long id);
}
