package com.flowboard.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@FeignClient(name = "notification-service", path = "/api/notifications")
public interface NotificationClient {
    
    @PostMapping("/send")
    Map<String, Object> sendNotification(@RequestBody Map<String, Object> request);
}
