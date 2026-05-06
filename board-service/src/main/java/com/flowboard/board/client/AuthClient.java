package com.flowboard.board.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/auth/users/{userId}")
    UserResponse getUserById(@PathVariable("userId") Long userId, 
                             @RequestHeader("Authorization") String token);

    @Data
    class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
    }
}
