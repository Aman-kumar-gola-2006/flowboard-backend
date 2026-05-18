package com.flowboard.notification.client;

import org.springframework.cloud.openfeign.FeignClient;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@FeignClient(name = "auth-service", path = "/api/auth")
public interface AuthClient {

    @GetMapping("/users/email")
    Map<String, Object> getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/internal/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);
}
