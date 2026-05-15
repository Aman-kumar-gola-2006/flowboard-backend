package com.flowboard.card.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@FeignClient(name = "board-service", path = "/api/boards")
public interface BoardClient {
    
    @GetMapping("/{id}")
    Map<String, Object> getBoardById(@PathVariable("id") Long id, 
                                     @RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("Authorization") String token);
}
