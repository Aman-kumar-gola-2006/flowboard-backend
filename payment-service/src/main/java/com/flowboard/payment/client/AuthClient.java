package com.flowboard.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "auth-service", path = "/api/auth")
public interface AuthClient {
    
    @PutMapping("/users/{id}/upgrade")
    Object upgradeUser(@PathVariable Long id);
}
