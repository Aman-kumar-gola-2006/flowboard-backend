package com.flowboard.board.repository;

import com.flowboard.board.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Simple repo for chat messages - keeping it minimal
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Load all messages for a board ordered by time (oldest first)
    List<ChatMessage> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    // Get last 50 messages for history load - newest first then reverse in service
    List<ChatMessage> findTop50ByBoardIdOrderByCreatedAtDesc(Long boardId);

    // Cleanup when board is deleted
    void deleteByBoardId(Long boardId);
}
