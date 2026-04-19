package com.flowboard.card.repository;

import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    List<Card> findByListIdOrderByPositionAsc(Long listId);
    
    List<Card> findByListIdAndIsArchivedOrderByPositionAsc(Long listId, Boolean isArchived);
    
    List<Card> findByBoardIdOrderByPositionAsc(Long boardId);
    
    List<Card> findByAssigneeId(Long assigneeId);
    
    List<Card> findByListIdAndPriority(Long listId, Priority priority);
    
    List<Card> findByListIdAndStatus(Long listId, Status status);
    
    List<Card> findByDueDateBeforeAndStatusNot(LocalDate date, Status status);
    
    @Query("SELECT MAX(c.position) FROM Card c WHERE c.listId = :listId")
    Integer findMaxPositionByListId(@Param("listId") Long listId);
    
    @Query("SELECT COUNT(c) FROM Card c WHERE c.listId = :listId")
    Long countByListId(@Param("listId") Long listId);
    
    List<Card> findByBoardIdAndIsArchivedFalse(Long boardId);
    
    List<Card> findByListIdAndIsArchivedTrue(Long listId);
}
