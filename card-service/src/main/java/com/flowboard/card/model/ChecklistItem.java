package com.flowboard.card.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "checklist_items")
@Getter
@Setter
public class ChecklistItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(nullable = false)
    private String text;
    
    @Column(name = "is_completed")
    private Boolean isCompleted = false;
    
    @Column(name = "position_index")
    private Integer position = 0;
}
