package com.flowboard.list.repository;

import com.flowboard.list.model.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ListRepository extends JpaRepository<TaskList, Long> {
    
    // Get all lists for a board, ordered by position
    List<TaskList> findByBoardIdOrderByPositionAsc(Long boardId);
    
    // Get only non-archived lists
    List<TaskList> findByBoardIdAndIsArchivedOrderByPositionAsc(Long boardId, Boolean isArchived);
    
    // Get archived lists
    List<TaskList> findByBoardIdAndIsArchivedTrue(Long boardId);
    
    // Find max position for a board (for adding new list at end)
    @Query("SELECT MAX(l.position) FROM TaskList l WHERE l.boardId = :boardId")
    Integer findMaxPositionByBoardId(@Param("boardId") Long boardId);
    
    // Count lists in a board
    Long countByBoardId(Long boardId);
    
    // Delete all lists in a board (when board is deleted)
    @Modifying
    @Query("DELETE FROM TaskList l WHERE l.boardId = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}
