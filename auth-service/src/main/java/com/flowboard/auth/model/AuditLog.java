package com.flowboard.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "actor_id")
    private Long actorId;
    
    @Column(name = "actor_name")
    private String actorName;
    
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, SUSPEND
    
    @Column(name = "entity_type")
    private String entityType; // USER, WORKSPACE, BOARD, CARD
    
    @Column(name = "entity_id")
    private Long entityId;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
