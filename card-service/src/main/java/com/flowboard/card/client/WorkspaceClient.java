package com.flowboard.card.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@FeignClient(name = "workspace-service", path = "/api/workspaces")
public interface WorkspaceClient {
    
    @GetMapping("/{id}")
    Map<String, Object> getWorkspaceById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @GetMapping("/{workspaceId}/members/{userId}/is-admin")
    Boolean isWorkspaceAdmin(@PathVariable("workspaceId") Long workspaceId, @PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);
}
