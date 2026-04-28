package com.flowboard.card.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_activity")
@Getter
@Setter
public class CardActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(name = "actor_id", nullable = false)
    private Long actorId;
    
    @Column(name = "actor_name")
    private String actorName;
    
    @Column(nullable = false)
    private String action; // ASSIGNED, STATUS_CHANGED, DUE_DATE_UPDATED, MOVED, CREATED, UPDATED
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
