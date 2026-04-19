package com.flowboard.comment.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    // null means top-level comment, otherwise it's a reply
    @Column(name = "parent_comment_id")
    private Long parentCommentId;
    
    @Column(name = "is_edited")
    private Boolean isEdited = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
