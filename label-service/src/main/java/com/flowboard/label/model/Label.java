package com.flowboard.label.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "labels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"board_id", "name"})
})
@Getter
@Setter
@ToString
public class Label {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "board_id", nullable = false)
    private Long boardId;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(length = 20)
    private String color = "#cccccc";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
