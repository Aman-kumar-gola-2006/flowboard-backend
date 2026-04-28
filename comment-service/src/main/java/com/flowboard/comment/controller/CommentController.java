package com.flowboard.comment.controller;

import com.flowboard.comment.dto.CommentRequest;
import com.flowboard.comment.dto.CommentResponse;
import com.flowboard.comment.dto.MessageResponse;
import com.flowboard.comment.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody CommentRequest request,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        System.out.println("DEBUG: addComment called");
        System.out.println("DEBUG: userId: " + userId);
        System.out.println("DEBUG: auth: " + (authorization != null ? "PRESENT" : "MISSING"));
        
        if (userId == null) {
            return ResponseEntity.badRequest().body(errorResponse("X-User-Id header is missing"));
        }
        
        try {
            CommentResponse response = commentService.addComment(request, userId, authorization);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/card/{cardId}")
    public ResponseEntity<?> getCommentsByCard(@PathVariable Long cardId,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            List<CommentResponse> comments = commentService.getCommentsByCard(cardId, userId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                           @RequestBody Map<String, String> payload,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            String content = payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Content cannot be empty");
            }
            CommentResponse response = commentService.updateComment(commentId, content, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            commentService.deleteComment(commentId, userId);
            return ResponseEntity.ok(new MessageResponse("Comment deleted", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/card/{cardId}/count")
    public ResponseEntity<?> getCommentCount(@PathVariable Long cardId,
                                             @RequestHeader("X-User-Id") Long userId) {
        try {
            Long count = commentService.getCommentCount(cardId, userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
