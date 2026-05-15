package com.flowboard.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "workspace-service", url = "http://3.110.61.209:8082", path = "/api/workspaces")
public interface WorkspaceClient {
    
    @GetMapping("/{workspaceId}/members/{userId}/exists")
    Boolean checkMembership(@PathVariable("workspaceId") Long workspaceId, @PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);
    
    @GetMapping("/{workspaceId}/members/{userId}/is-admin")
    Boolean isWorkspaceAdmin(@PathVariable("workspaceId") Long workspaceId, @PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);

    @GetMapping("/{id}")
    java.util.Map<String, Object> getWorkspaceById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @GetMapping("/{workspaceId}/members")
    java.util.List<java.util.Map<String, Object>> getWorkspaceMembers(@PathVariable("workspaceId") Long workspaceId, @RequestHeader("Authorization") String token);
}
