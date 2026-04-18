package com.flowboard.list.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_lists")
@Getter
@Setter
@ToString
public class TaskList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "board_id", nullable = false)
    private Long boardId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    // Position for ordering lists left to right
    @Column(name = "position_index")
    private Integer position = 0;
    
    @Column(name = "color", length = 20)
    private String color = "#dddddd";
    
    @Column(name = "is_archived")
    private Boolean isArchived = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // No relationships to avoid JSON serialization issues
    // I'll handle card count separately when needed
}
