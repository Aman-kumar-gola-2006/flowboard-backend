package com.flowboard.auth.service;

import com.flowboard.auth.model.AuditLog;
import com.flowboard.auth.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditLogService {
    
    @Autowired
    private AuditLogRepository auditRepo;
    
    public void log(Long actorId, String actorName, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorName(actorName);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditRepo.save(log);
        System.out.println("Audit: " + actorName + " " + action + " " + entityType);
    }
    
    public List<AuditLog> getAllLogs() {
        return auditRepo.findByOrderByCreatedAtDesc();
    }
}
