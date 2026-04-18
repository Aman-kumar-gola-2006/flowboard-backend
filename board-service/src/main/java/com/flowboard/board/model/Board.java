package com.flowboard.board.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "boards")
@Getter
@Setter
@ToString(exclude = "members")  // EXCLUDE members from toString
public class Board {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "background_color", length = 20)
    private String backgroundColor = "#ffffff";
    
    @Column(name = "visibility", length = 20)
    private String visibility = "PRIVATE";
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "is_closed")
    private Boolean isClosed = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @JsonIgnore
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BoardMember> members = new HashSet<>();
    
    public void addMember(Long userId, String role) {
        BoardMember member = new BoardMember();
        member.setBoard(this);
        member.setUserId(userId);
        member.setRole(role);
        this.members.add(member);
    }
}
