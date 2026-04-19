package com.flowboard.comment.repository;

import com.flowboard.comment.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Get all comments for a card, sorted old to new
    List<Comment> findByCardIdOrderByCreatedAtAsc(Long cardId);
    
    // Get only top-level comments (no parent)
    List<Comment> findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(Long cardId);
    
    // Get replies for a specific comment
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);
    
    // Count comments on a card
    Long countByCardId(Long cardId);
}
