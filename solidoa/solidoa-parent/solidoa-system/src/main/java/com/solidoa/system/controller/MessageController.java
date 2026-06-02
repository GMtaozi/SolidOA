package com.solidoa.system.controller;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.MessageDTO;
import com.solidoa.system.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/messages")
@Validated
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    public Result<Void> sendMessage(@RequestBody MessageDTO message) {
        messageService.sendMessage(message);
        return Result.success();
    }

    @GetMapping("/unread/{userId}")
    public Result<Integer> getUnreadCount(@PathVariable Long userId) {
        return Result.success(messageService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success();
    }
}
