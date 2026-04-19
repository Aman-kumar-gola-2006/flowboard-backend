package com.flowboard.comment.service;

import com.flowboard.comment.client.CardClient;
import com.flowboard.comment.dto.CommentRequest;
import com.flowboard.comment.dto.CommentResponse;
import com.flowboard.comment.model.Comment;
import com.flowboard.comment.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    
    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    
    @Autowired
    private CommentRepository commentRepo;
    
    @Autowired
    private CardClient cardClient;
    
    /**
     * Add a comment (or reply) to a card
     */
    @Transactional
    public CommentResponse addComment(CommentRequest request, Long userId, String authorization) {
        
        log.info("User {} adding comment to card {}", userId, request.getCardId());
        
        // Check if user has access to the card
        try {
            cardClient.getCardById(request.getCardId(), userId, authorization);
        } catch (Exception e) {
            throw new RuntimeException("Card not found or access denied");
        }
        
        // If it's a reply, verify parent comment exists and belongs to same card
        if (request.getParentCommentId() != null) {
            Comment parent = commentRepo.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (!parent.getCardId().equals(request.getCardId())) {
                throw new RuntimeException("Parent comment does not belong to this card");
            }
        }
        
        Comment comment = new Comment();
        comment.setCardId(request.getCardId());
        comment.setAuthorId(userId);
        comment.setContent(request.getContent());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setIsEdited(false);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        
        Comment saved = commentRepo.save(comment);
        log.info("Comment {} added", saved.getId());
        
        return mapToResponse(saved);
    }
    
    /**
     * Get all comments for a card with nested replies
     */
    public List<CommentResponse> getCommentsByCard(Long cardId, Long userId) {
        
        log.info("Fetching comments for card {}", cardId);
        
        // Verify access
        try {
            cardClient.getCardById(cardId, userId, null);
        } catch (Exception e) {
            throw new RuntimeException("Card not found or access denied");
        }
        
        // Get top-level comments
        List<Comment> topLevelComments = commentRepo.findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(cardId);
        
        return topLevelComments.stream()
                .map(this::mapToResponseWithReplies)
                .collect(Collectors.toList());
    }
    
    /**
     * Update a comment - only author can edit
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, String content, Long userId) {
        
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        // Check access to card
        try {
            cardClient.getCardById(comment.getCardId(), userId, content);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        // Only author can edit
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("You can only edit your own comments");
        }
        
        comment.setContent(content);
        comment.setIsEdited(true);
        comment.setUpdatedAt(LocalDateTime.now());
        
        Comment updated = commentRepo.save(comment);
        log.info("Comment {} updated", commentId);
        
        return mapToResponse(updated);
    }
    
    /**
     * Delete a comment - only author can delete
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        // Check access
        try {
            cardClient.getCardById(comment.getCardId(), userId, null);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        // Only author can delete (or board admin - TODO)
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("You can only delete your own comments");
        }
        
        // Delete replies first (cascade manually)
        List<Comment> replies = commentRepo.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            commentRepo.delete(reply);
        }
        
        commentRepo.delete(comment);
        log.info("Comment {} and its replies deleted", commentId);
    }
    
    /**
     * Get comment count for a card
     */
    public Long getCommentCount(Long cardId, Long userId) {
        try {
            cardClient.getCardById(cardId, userId, null);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        return commentRepo.countByCardId(cardId);
    }
    
    // Helper to map entity to response (without replies)
    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .cardId(comment.getCardId())
                .authorId(comment.getAuthorId())
                .authorName("User " + comment.getAuthorId()) // TODO: fetch from Auth Service
                .content(comment.getContent())
                .parentCommentId(comment.getParentCommentId())
                .isEdited(comment.getIsEdited())
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }
    
    // Helper that includes nested replies
    private CommentResponse mapToResponseWithReplies(Comment comment) {
        CommentResponse response = mapToResponse(comment);
        
        // Get replies for this comment
        List<Comment> replies = commentRepo.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::mapToResponseWithReplies) // recursive for nested replies
                .collect(Collectors.toList());
        
        response.setReplies(replyResponses);
        return response;
    }
}
