package com.flowboard.board.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "labels")
@Getter
@Setter
public class Label {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "board_id", nullable = false)
    private Long boardId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String color;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
