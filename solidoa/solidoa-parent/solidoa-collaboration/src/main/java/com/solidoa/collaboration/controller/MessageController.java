package com.solidoa.collaboration.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.collaboration.form.SendMessageForm;
import com.solidoa.collaboration.dto.MessageDTO;
import com.solidoa.collaboration.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collab")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/messages")
    public Result<Long> send(@RequestBody @Valid SendMessageForm form,
                              @RequestHeader("X-User-Id") Long senderId) {
        return Result.success(messageService.send(form, senderId));
    }

    @GetMapping("/messages")
    public Result<PageVO<MessageDTO>> list(PageDTO dto,
                                          @RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.pageList(dto, userId));
    }

    @GetMapping("/messages/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    @PutMapping("/messages/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.success();
    }

    @PutMapping("/messages/read-all")
    public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        messageService.markAllRead(userId);
        return Result.success();
    }
}