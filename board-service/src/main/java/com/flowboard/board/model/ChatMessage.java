package com.flowboard.board.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Simple chat message entity for board real-time chat
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_board_time", columnList = "board_id, created_at")
})
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which board this message belongs to
    @Column(name = "board_id", nullable = false)
    private Long boardId;

    // Who sent it
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "sender_avatar", length = 500)
    private String senderAvatar;

    // The actual message text
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
