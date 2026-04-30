package com.flowboard.workspace.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_invitations")
@Getter
@Setter
public class WorkspaceInvitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long workspaceId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(nullable = false)
    private String role = "MEMBER";
    
    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
    
    @Column(name = "is_accepted")
    private Boolean isAccepted = false;

    public WorkspaceInvitation() {
        this.token = UUID.randomUUID().toString();
    }
}
