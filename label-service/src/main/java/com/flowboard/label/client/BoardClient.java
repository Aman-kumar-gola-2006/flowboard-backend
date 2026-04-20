package com.flowboard.label.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "board-client", url = "${board.service.url:http://localhost:8083/api/boards}")
public interface BoardClient {
    
    @GetMapping("/{boardId}")
    Object getBoardById(@PathVariable Long boardId, 
                        @RequestHeader("X-User-Id") Long userId);
}
