package com.flowboard.label.controller;

import com.flowboard.label.model.Label;
import com.flowboard.label.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/labels")
@CrossOrigin(origins = "*")
public class LabelController {
    
    @Autowired
    private LabelService labelService;
    
    /**
     * Get all labels for a board
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<?> getBoardLabels(@PathVariable Long boardId,
                                            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<Label> labels = labelService.getBoardLabels(boardId, userId);
            return ResponseEntity.ok(labels);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Create a new label
     */
    @PostMapping("/board/{boardId}")
    public ResponseEntity<?> createLabel(@PathVariable Long boardId,
                                         @RequestBody Map<String, String> payload,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            String name = payload.get("name");
            String color = payload.get("color");
            
            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Label name is required");
            }
            
            Label label = labelService.createLabel(boardId, name, color, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(label);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update a label
     */
    @PutMapping("/{labelId}")
    public ResponseEntity<?> updateLabel(@PathVariable Long labelId,
                                         @RequestBody Map<String, String> payload,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            String name = payload.get("name");
            String color = payload.get("color");
            
            Label label = labelService.updateLabel(labelId, name, color, userId);
            return ResponseEntity.ok(label);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Delete a label
     */
    @DeleteMapping("/{labelId}")
    public ResponseEntity<?> deleteLabel(@PathVariable Long labelId,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            labelService.deleteLabel(labelId, userId);
            return ResponseEntity.ok(Map.of("message", "Label deleted", "success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Add label to card
     */
    @PostMapping("/card/{cardId}/add/{labelId}")
    public ResponseEntity<?> addLabelToCard(@PathVariable Long cardId,
                                            @PathVariable Long labelId,
                                            @RequestParam Long boardId,
                                            @RequestHeader("X-User-Id") Long userId) {
        try {
            labelService.addLabelToCard(cardId, labelId, boardId, userId);
            return ResponseEntity.ok(Map.of("message", "Label added to card", "success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Remove label from card
     */
    @DeleteMapping("/card/{cardId}/remove/{labelId}")
    public ResponseEntity<?> removeLabelFromCard(@PathVariable Long cardId,
                                                 @PathVariable Long labelId,
                                                 @RequestParam Long boardId,
                                                 @RequestHeader("X-User-Id") Long userId) {
        try {
            labelService.removeLabelFromCard(cardId, labelId, boardId, userId);
            return ResponseEntity.ok(Map.of("message", "Label removed from card", "success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all labels on a card
     */
    @GetMapping("/card/{cardId}")
    public ResponseEntity<?> getCardLabels(@PathVariable Long cardId,
                                           @RequestParam Long boardId,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            List<Label> labels = labelService.getCardLabels(cardId, boardId, userId);
            return ResponseEntity.ok(labels);
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
