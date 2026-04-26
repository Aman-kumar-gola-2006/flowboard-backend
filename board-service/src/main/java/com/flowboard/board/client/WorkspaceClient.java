package com.flowboard.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// FIXED: Direct URL with localhost instead of service name
@FeignClient(name = "workspace-service", path = "/api/workspaces")
public interface WorkspaceClient {
    
    @GetMapping("/{workspaceId}/members/{userId}/exists")
    Boolean checkMembership(@PathVariable Long workspaceId, @PathVariable Long userId);
    
    @GetMapping("/{workspaceId}/members/{userId}/is-admin")
    Boolean isWorkspaceAdmin(@PathVariable Long workspaceId, @PathVariable Long userId);
}
