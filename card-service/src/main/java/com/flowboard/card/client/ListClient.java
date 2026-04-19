package com.flowboard.card.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "list-client", url = "${list.service.url:http://localhost:8084/api/lists}")
public interface ListClient {
    
    @GetMapping("/{listId}")
    Object getListById(@PathVariable Long listId, 
                       @RequestHeader("X-User-Id") Long userId);
}
