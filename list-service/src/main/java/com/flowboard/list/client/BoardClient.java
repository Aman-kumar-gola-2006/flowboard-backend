package com.flowboard.list.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Talks to Board Service to check permissions.
 * Simple Feign client - no fallback for now.
 */
@FeignClient(name = "board-client", url = "${board.service.url:http://localhost:8083/api/boards}")
public interface BoardClient {
    
    // Check if user has access to the board
    // We don't actually have this endpoint in Board Service yet, 
    // so we'll just check if board exists for now
    // TODO: Add proper permission endpoint in Board Service
    @GetMapping("/{boardId}")
    Object getBoardById(@PathVariable Long boardId, 
                        @RequestHeader("X-User-Id") Long userId);
}
