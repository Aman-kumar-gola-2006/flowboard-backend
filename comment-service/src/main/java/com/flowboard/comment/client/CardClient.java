package com.flowboard.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// Talks to Card Service to verify access
@FeignClient(name = "card-client", url = "${card.service.url:http://localhost:8085/api/cards}")
public interface CardClient {
    
    @GetMapping("/{cardId}")
    Object getCardById(@PathVariable Long cardId, 
                       @RequestHeader("X-User-Id") Long userId,
                       @RequestHeader("Authorization") String authorization);
}
