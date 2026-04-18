package com.flowboard.board.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"board_id", "user_id"})
})
@Getter
@Setter
@ToString(exclude = "board")  // EXCLUDE board from toString
public class BoardMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(length = 20)
    private String role = "MEMBER";
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();
}
