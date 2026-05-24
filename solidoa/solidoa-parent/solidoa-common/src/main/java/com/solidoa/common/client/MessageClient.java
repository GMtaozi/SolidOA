package com.solidoa.common.client;

import com.solidoa.common.result.Result;
import com.solidoa.common.vo.MessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "collaboration-service", path = "/api/v1/collab")
public interface MessageClient {

    @PostMapping("/messages")
    Result<Void> sendMessage(@RequestBody MessageDTO message);

    @GetMapping("/messages/unread/{userId}")
    Result<Integer> getUnreadCount(@PathVariable("userId") Long userId);
}