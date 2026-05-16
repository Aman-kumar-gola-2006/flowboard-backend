package com.flowboard.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "workspace-service", path = "/api/workspaces")
public interface WorkspaceClient {
    
    @PutMapping("/{id}/upgrade")
    Object upgradeWorkspace(@PathVariable("id") Long id);
}
