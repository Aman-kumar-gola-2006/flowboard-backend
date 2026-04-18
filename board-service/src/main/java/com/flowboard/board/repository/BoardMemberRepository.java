package com.flowboard.board.repository;

import com.flowboard.board.model.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {
    
    // Get all members of a board
    List<BoardMember> findByBoardId(Long boardId);
    
    // Find specific member
    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);
    
    // Check if user is member
    boolean existsByBoardIdAndUserId(Long boardId, Long userId);
    
    // Remove member from board
    @Modifying
    @Query("DELETE FROM BoardMember m WHERE m.board.id = :boardId AND m.userId = :userId")
    void deleteByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);
    
    // Get user's role in board
    @Query("SELECT m.role FROM BoardMember m WHERE m.board.id = :boardId AND m.userId = :userId")
    String findRoleByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);
}
