package com.flowboard.card.repository;

import com.flowboard.card.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByCardIdOrderByPositionAsc(Long cardId);
    Long countByCardId(Long cardId);
    Long countByCardIdAndIsCompletedTrue(Long cardId);
    void deleteByCardId(Long cardId);
}
