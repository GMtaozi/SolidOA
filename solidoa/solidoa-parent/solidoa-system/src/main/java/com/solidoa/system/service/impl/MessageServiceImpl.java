package com.solidoa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.MessageDTO;
import com.solidoa.system.entity.Message;
import com.solidoa.system.mapper.MessageMapper;
import com.solidoa.system.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void sendMessage(MessageDTO dto) {
        if (dto.getReceiverId() == null) {
            throw new BusinessException("接收人不能为空");
        }
        Message message = new Message();
        BeanUtils.copyProperties(dto, message);
        if (message.getMsgNo() == null) {
            message.setMsgNo(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        }
        if (message.getIsRead() == null) {
            message.setIsRead(0);
        }
        if (message.getPriority() == null) {
            message.setPriority(0);
        }
        messageMapper.insert(message);
        log.info("发送消息: id={}, type={}, receiverId={}", message.getId(), message.getType(), message.getReceiverId());
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId).eq(Message::getIsRead, 0);
        return Math.toIntExact(messageMapper.selectCount(wrapper));
    }

    @Override
    public void markAsRead(Long id) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        message.setIsRead(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
        log.info("消息已读: id={}", id);
    }
}
