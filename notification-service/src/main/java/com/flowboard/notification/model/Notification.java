package com.flowboard.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@ToString
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;
    
    @Column(name = "actor_id")
    private Long actorId;
    
    @Column(nullable = false)
    private String type; // ASSIGNMENT, MENTION, DUE_DATE, COMMENT, MOVE
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "related_id")
    private Long relatedId; // cardId, boardId, etc.
    
    @Column(name = "related_type")
    private String relatedType; // CARD, BOARD, COMMENT
    
    @Column(name = "deep_link")
    private String deepLink;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "is_email_sent")
    private Boolean isEmailSent = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
