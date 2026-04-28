package com.flowboard.card.controller;

import com.flowboard.card.dto.CardRequest;
import com.flowboard.card.dto.CardResponse;
import com.flowboard.card.dto.MessageResponse;
import com.flowboard.card.dto.MoveCardRequest;
import com.flowboard.card.enums.Status;
import com.flowboard.card.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.flowboard.card.model.Attachment;
import com.flowboard.card.model.CardActivity;
import com.flowboard.card.model.ChecklistItem;
import com.flowboard.card.repository.AttachmentRepository;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.ChecklistItemRepository;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "*")
public class CardController {
    
    @Autowired
    private CardService cardService;
    
    @Autowired
    private ChecklistItemRepository checklistRepo;

    @Autowired
    private CardActivityRepository activityRepo;

    @Autowired
    private AttachmentRepository attachmentRepo;

    @GetMapping("/{cardId}/checklist")
    public ResponseEntity<List<ChecklistItem>> getChecklist(@PathVariable Long cardId) {
        return ResponseEntity.ok(checklistRepo.findByCardIdOrderByPositionAsc(cardId));
    }

    @GetMapping("/{cardId}/activity")
    public ResponseEntity<List<CardActivity>> getCardActivity(@PathVariable Long cardId) {
        return ResponseEntity.ok(activityRepo.findByCardIdOrderByCreatedAtDesc(cardId));
    }

    @PostMapping("/{cardId}/attachments")
    public ResponseEntity<Attachment> uploadAttachment(@PathVariable Long cardId, 
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            // Save to local folder
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);
            
            Attachment attachment = new Attachment();
            attachment.setCardId(cardId);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFileUrl("/uploads/" + fileName);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setUploadedBy(userId);
            
            return ResponseEntity.ok(attachmentRepo.save(attachment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{cardId}/attachments")
    public ResponseEntity<List<Attachment>> getAttachments(@PathVariable Long cardId) {
        return ResponseEntity.ok(attachmentRepo.findByCardIdOrderByCreatedAtDesc(cardId));
    }

    @DeleteMapping("/{cardId}/attachments/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long cardId, @PathVariable Long attachmentId) {
        attachmentRepo.deleteById(attachmentId);
        return ResponseEntity.ok(Map.of("message", "Attachment deleted"));
    }

    @PostMapping("/{cardId}/checklist")
    public ResponseEntity<ChecklistItem> addChecklistItem(@PathVariable Long cardId, @RequestBody Map<String, String> payload) {
        ChecklistItem item = new ChecklistItem();
        item.setCardId(cardId);
        item.setText(payload.get("text"));
        item.setPosition(checklistRepo.countByCardId(cardId).intValue());
        return ResponseEntity.ok(checklistRepo.save(item));
    }

    @PutMapping("/{cardId}/checklist/{itemId}")
    public ResponseEntity<ChecklistItem> toggleChecklistItem(@PathVariable Long cardId, @PathVariable Long itemId) {
        ChecklistItem item = checklistRepo.findById(itemId).orElseThrow();
        item.setIsCompleted(!item.getIsCompleted());
        return ResponseEntity.ok(checklistRepo.save(item));
    }

    @DeleteMapping("/{cardId}/checklist/{itemId}")
    public ResponseEntity<?> deleteChecklistItem(@PathVariable Long cardId, @PathVariable Long itemId) {
        checklistRepo.deleteById(itemId);
        return ResponseEntity.ok(Map.of("message", "Item deleted"));
    }
    
    @PostMapping
    public ResponseEntity<?> createCard(@RequestBody CardRequest request,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            CardResponse response = cardService.createCard(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/list/{listId}")
    public ResponseEntity<?> getCardsByList(@PathVariable Long listId,
                                            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<CardResponse> cards = cardService.getCardsByList(listId, userId);
            return ResponseEntity.ok(cards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{cardId}")
    public ResponseEntity<?> getCardById(@PathVariable Long cardId,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            CardResponse card = cardService.getCardById(cardId, userId);
            return ResponseEntity.ok(card);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/board/{boardId}")
    public ResponseEntity<?> getCardsByBoard(@PathVariable Long boardId,
                                             @RequestHeader("X-User-Id") Long userId) {
        try {
            List<CardResponse> cards = cardService.getCardsByBoard(boardId, userId);
            return ResponseEntity.ok(cards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{cardId}")
    public ResponseEntity<?> updateCard(@PathVariable Long cardId,
                                        @RequestBody CardRequest request,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            CardResponse response = cardService.updateCard(cardId, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{cardId}/status")
    public ResponseEntity<?> updateCardStatus(@PathVariable Long cardId,
                                              @RequestBody Map<String, String> payload,
                                              @RequestHeader("X-User-Id") Long userId) {
        try {
            Status status = Status.valueOf(payload.get("status"));
            cardService.updateCardStatus(cardId, status, userId);
            return ResponseEntity.ok(new MessageResponse("Status updated", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{cardId}/move")
    public ResponseEntity<?> moveCard(@PathVariable Long cardId,
                                      @RequestBody MoveCardRequest request,
                                      @RequestHeader("X-User-Id") Long userId) {
        try {
            List<CardResponse> cards = cardService.moveCard(cardId, request, userId);
            return ResponseEntity.ok(cards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{cardId}/archive")
    public ResponseEntity<?> archiveCard(@PathVariable Long cardId,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            cardService.archiveCard(cardId, userId);
            return ResponseEntity.ok(new MessageResponse("Card archived", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{cardId}")
    public ResponseEntity<?> deleteCard(@PathVariable Long cardId,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            cardService.deleteCard(cardId, userId);
            return ResponseEntity.ok(new MessageResponse("Card deleted", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/assignee/{userId}")
    public ResponseEntity<?> getCardsByAssignee(@PathVariable Long userId,
                                                @RequestHeader("X-User-Id") Long requestUserId) {
        try {
            List<CardResponse> cards = cardService.getCardsByAssignee(userId, requestUserId);
            return ResponseEntity.ok(cards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/board/{boardId}/overdue")
    public ResponseEntity<?> getOverdueCards(@PathVariable Long boardId,
                                             @RequestHeader("X-User-Id") Long userId) {
        try {
            List<CardResponse> cards = cardService.getOverdueCards(boardId, userId);
            return ResponseEntity.ok(cards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCards() {
        return ResponseEntity.ok(cardService.getTotalCount());
    }

    @GetMapping("/overdue/all")
    public ResponseEntity<?> getAllOverdueCards() {
        return ResponseEntity.ok(cardService.getAllOverdueCards());
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
