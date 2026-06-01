package com.solidoa.common.client;

import com.solidoa.common.config.FeignConfig;
import com.solidoa.common.result.Result;
import com.solidoa.common.vo.MessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "system-service",
    contextId = "messageClient",
    path = "/api/v1/system",
    configuration = FeignConfig.class
)
public interface MessageClient {

    @PostMapping("/messages")
    Result<Void> sendMessage(@RequestBody MessageDTO message);

    @GetMapping("/messages/unread/{userId}")
    Result<Integer> getUnreadCount(@PathVariable("userId") Long userId);
}