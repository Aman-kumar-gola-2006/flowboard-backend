package com.flowboard.label.repository;

import com.flowboard.label.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    
    List<Label> findByBoardId(Long boardId);
    
    Optional<Label> findByBoardIdAndName(Long boardId, String name);
    
    void deleteByBoardId(Long boardId);
    
    boolean existsByBoardIdAndName(Long boardId, String name);
}
