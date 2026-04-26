package com.flowboard.workspace.client;

import com.flowboard.workspace.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "auth-service", path = "/api/auth")
public interface AuthServiceClient {
    
    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable Long id, @RequestHeader("Authorization") String token);
    
    @GetMapping("/users/email/{email}")
    UserResponse getUserByEmail(@PathVariable String email, @RequestHeader("Authorization") String token);
    
    @GetMapping("/users/search")
    List<UserResponse> searchUsers(@RequestParam String name, @RequestHeader("Authorization") String token);
    
    @GetMapping("/me")
    UserResponse getCurrentUser(@RequestHeader("Authorization") String token);
}
