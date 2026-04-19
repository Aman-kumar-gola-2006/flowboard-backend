package com.flowboard.card.model;

import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "list_id", nullable = false)
    private Long listId;
    
    @Column(name = "board_id", nullable = false)
    private Long boardId;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "position_index")
    private Integer position = 0;
    
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.TO_DO;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "assignee_id")
    private Long assigneeId;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "cover_color", length = 20)
    private String coverColor = "#ffffff";
    
    @Column(name = "is_archived")
    private Boolean isArchived = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
