package com.flowboard.card.client;

import com.flowboard.card.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "http://3.110.61.209.nip.io:8081", path = "/api/auth")
public interface AuthClient {
    
    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);
}
