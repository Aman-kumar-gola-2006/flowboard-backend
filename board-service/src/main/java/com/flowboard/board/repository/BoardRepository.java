package com.flowboard.board.repository;

import com.flowboard.board.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    
    // Get all boards in a workspace
    List<Board> findByWorkspaceId(Long workspaceId);
    
    // Get boards created by a user
    List<Board> findByCreatedBy(Long userId);
    
    // Get active boards only
    List<Board> findByWorkspaceIdAndIsClosed(Long workspaceId, Boolean isClosed);
    
    // Check if board name already exists in workspace
    boolean existsByNameAndWorkspaceId(String name, Long workspaceId);
    
    // Custom query to get boards where user is a member
    @Query("SELECT b FROM Board b JOIN b.members m WHERE m.userId = :userId AND b.isClosed = false")
    List<Board> findBoardsByMemberUserId(@Param("userId") Long userId);
    
    // Count boards in workspace - for analytics
    Long countByWorkspaceId(Long workspaceId);
}
