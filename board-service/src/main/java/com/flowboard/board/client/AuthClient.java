package com.flowboard.board.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", path = "/api/auth")
public interface AuthClient {
    
    @GetMapping("/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @GetMapping("/internal/users/{id}")
    Map<String, Object> getInternalUserById(@PathVariable("id") Long id);
}
