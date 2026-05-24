package com.solidoa.collaboration.service.impl;

import com.solidoa.collaboration.form.SendMessageForm;
import com.solidoa.collaboration.dto.MessageDTO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.collaboration.service.MessageService;
import com.solidoa.collaboration.mapper.MessageMapper;
import com.solidoa.collaboration.entity.Message;
import com.solidoa.collaboration.handler.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private final AtomicLong counter = new AtomicLong(1);

    @Override
    @Transactional
    public Long send(SendMessageForm form, Long senderId) {
        Message message = new Message();
        message.setMsgNo(generateMsgNo());
        message.setTitle(form.getTitle());
        message.setContent(form.getContent());
        message.setType(form.getType() != null ? form.getType() : "SYSTEM");
        message.setPriority(form.getPriority() != null ? form.getPriority() : 0);
        message.setSenderId(senderId);
        message.setReceiverId(form.getReceiverId());
        message.setRelatedType(form.getRelatedType());
        message.setRelatedId(form.getRelatedId());
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insert(message);

        sendToUser(form.getReceiverId(), "MESSAGE", form.getTitle(), form.getContent());

        return message.getId();
    }

    @Override
    public PageVO<MessageDTO> pageList(PageDTO dto, Long userId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<Message> messages = messageMapper.selectPageList(offset, dto.getPageSize(), userId);
        long total = messageMapper.selectCount(userId);

        List<MessageDTO> records = new ArrayList<>();
        for (Message msg : messages) {
            MessageDTO dto2 = new MessageDTO();
            dto2.setId(msg.getId());
            dto2.setMsgNo(msg.getMsgNo());
            dto2.setTitle(msg.getTitle());
            dto2.setContent(msg.getContent());
            dto2.setType(msg.getType());
            dto2.setPriority(msg.getPriority());
            dto2.setIsRead(msg.getIsRead());
            dto2.setRelatedType(msg.getRelatedType());
            dto2.setRelatedId(msg.getRelatedId());
            dto2.setCreateTime(msg.getCreateTime().toString());
            records.add(dto2);
        }

        PageVO<MessageDTO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    public long getUnreadCount(Long userId) {
        return messageMapper.selectUnreadCount(userId);
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Message message = new Message();
        message.setId(id);
        message.setIsRead(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        List<Message> messages = messageMapper.selectPageList(0, Integer.MAX_VALUE, userId);
        for (Message msg : messages) {
            if (msg.getIsRead() == 0) {
                msg.setIsRead(1);
                msg.setReadTime(LocalDateTime.now());
                messageMapper.updateById(msg);
            }
        }
    }

    @Override
    public void sendToUser(Long userId, String type, String title, String content) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", type);
            wsMessage.put("title", title);
            wsMessage.put("content", content);
            wsMessage.put("timestamp", System.currentTimeMillis());

            webSocketHandler.sendToUser(userId, objectMapper.writeValueAsString(wsMessage));
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    private String generateMsgNo() {
        return "MSG" + System.currentTimeMillis() + String.format("%04d", counter.getAndIncrement());
    }
}