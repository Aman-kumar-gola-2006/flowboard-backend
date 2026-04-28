package com.flowboard.card.service;

import com.flowboard.card.client.ListClient;
import com.flowboard.card.dto.CardRequest;
import com.flowboard.card.dto.CardResponse;
import com.flowboard.card.dto.MoveCardRequest;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.model.Card;
import com.flowboard.card.model.CardActivity;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardService {
    
    private static final Logger log = LoggerFactory.getLogger(CardService.class);
    
    @Autowired
    private CardRepository cardRepo;
    
    @Autowired
    private ListClient listClient;

    @Autowired
    private CardActivityRepository activityRepo;
    
    @Transactional
    public CardResponse createCard(CardRequest request, Long userId) {
        
        log.info("Creating card '{}' in list {} by user {}", 
                 request.getTitle(), request.getListId(), userId);
        
        // Check list access
        try {
            listClient.getListById(request.getListId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("List not found or access denied");
        }
        
        // Get max position for the list
        Integer maxPos = cardRepo.findMaxPositionByListId(request.getListId());
        int newPosition = (maxPos != null) ? maxPos + 1 : 0;
        
        Card card = new Card();
        card.setListId(request.getListId());
        card.setBoardId(request.getBoardId());
        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        card.setPosition(newPosition);
        card.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        card.setStatus(Status.TO_DO);
        card.setDueDate(request.getDueDate());
        card.setStartDate(request.getStartDate());
        card.setAssigneeId(request.getAssigneeId());
        card.setCreatedBy(userId);
        card.setCoverColor(request.getCoverColor() != null ? request.getCoverColor() : "#ffffff");
        card.setIsArchived(false);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());
        
        Card saved = cardRepo.save(card);
        log.info("Card created with ID: {}", saved.getId());
        
        logActivity(saved.getId(), userId, "User", "CREATED", "Card created: " + saved.getTitle());
        
        return mapToResponse(saved);
    }
    
    public List<CardResponse> getCardsByList(Long listId, Long userId) {
        
        log.info("Fetching cards for list: {}", listId);
        
        try {
            listClient.getListById(listId, userId);
        } catch (Exception e) {
            throw new RuntimeException("List not found or access denied");
        }
        
        List<Card> cards = cardRepo.findByListIdAndIsArchivedOrderByPositionAsc(listId, false);
        return cards.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public CardResponse getCardById(Long cardId, Long userId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        try {
            listClient.getListById(card.getListId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        return mapToResponse(card);
    }
    
    @Transactional
    public CardResponse updateCard(Long cardId, CardRequest request, Long userId) {
        
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        try {
            listClient.getListById(card.getListId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        if (request.getTitle() != null) card.setTitle(request.getTitle());
        if (request.getDescription() != null) card.setDescription(request.getDescription());
        if (request.getPriority() != null) card.setPriority(request.getPriority());
        if (request.getStatus() != null) card.setStatus(request.getStatus());
        if (request.getDueDate() != null) card.setDueDate(request.getDueDate());
        if (request.getStartDate() != null) card.setStartDate(request.getStartDate());
        if (request.getAssigneeId() != null) card.setAssigneeId(request.getAssigneeId());
        if (request.getCoverColor() != null) card.setCoverColor(request.getCoverColor());
        
        card.setUpdatedAt(LocalDateTime.now());
        
        Card updated = cardRepo.save(card);
        logActivity(cardId, userId, "User", "UPDATED", "Card updated");
        return mapToResponse(updated);
    }
    
    @Transactional
    public void updateCardStatus(Long cardId, Status status, Long userId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        try {
            listClient.getListById(card.getListId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        card.setStatus(status);
        card.setUpdatedAt(LocalDateTime.now());
        cardRepo.save(card);
        logActivity(cardId, userId, "User", "STATUS_CHANGED", "Status changed to " + status);
    }
    
    @Transactional
    public List<CardResponse> moveCard(Long cardId, MoveCardRequest request, Long userId) {
        
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        Long sourceListId = card.getListId();
        Long targetListId = request.getTargetListId();
        
        // Check access to both lists
        try {
            listClient.getListById(sourceListId, userId);
            listClient.getListById(targetListId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied to one of the lists");
        }
        
        if (sourceListId.equals(targetListId)) {
            // Same list - just reorder
            reorderWithinList(cardId, request.getNewPosition(), sourceListId);
        } else {
            // Different list - remove from source, add to target
            moveToList(card, targetListId, request.getNewPosition());
        }
        
        card.setUpdatedAt(LocalDateTime.now());
        cardRepo.save(card);
        
        logActivity(cardId, userId, "User", "MOVED", "Card moved to list " + targetListId);
        
        return getCardsByList(targetListId, userId);
    }
    
    private void reorderWithinList(Long cardId, Integer newPosition, Long listId) {
        List<Card> cards = cardRepo.findByListIdOrderByPositionAsc(listId);
        
        Card movedCard = null;
        for (Card c : cards) {
            if (c.getId().equals(cardId)) {
                movedCard = c;
                break;
            }
        }
        
        if (movedCard == null) return;
        
        cards.remove(movedCard);
        cards.add(newPosition, movedCard);
        
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setPosition(i);
            cardRepo.save(cards.get(i));
        }
    }
    
    private void moveToList(Card card, Long targetListId, Integer newPosition) {
        // Remove from source list
        List<Card> sourceCards = cardRepo.findByListIdOrderByPositionAsc(card.getListId());
        sourceCards.removeIf(c -> c.getId().equals(card.getId()));
        for (int i = 0; i < sourceCards.size(); i++) {
            sourceCards.get(i).setPosition(i);
            cardRepo.save(sourceCards.get(i));
        }
        
        // Add to target list
        List<Card> targetCards = cardRepo.findByListIdOrderByPositionAsc(targetListId);
        card.setListId(targetListId);
        
        int insertPos = (newPosition != null && newPosition <= targetCards.size()) 
                        ? newPosition : targetCards.size();
        targetCards.add(insertPos, card);
        
        for (int i = 0; i < targetCards.size(); i++) {
            targetCards.get(i).setPosition(i);
            cardRepo.save(targetCards.get(i));
        }
    }
    
    @Transactional
    public void archiveCard(Long cardId, Long userId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        try {
            listClient.getListById(card.getListId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        card.setIsArchived(true);
        card.setUpdatedAt(LocalDateTime.now());
        cardRepo.save(card);
        log.info("Card {} archived", cardId);
    }
    
    @Transactional
    public void deleteCard(Long cardId, Long userId) {
        Card card = cardRepo.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        
        try {
            listClient.getListById(card.getListId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        cardRepo.delete(card);
        log.warn("Card {} permanently deleted", cardId);
    }
    
    public List<CardResponse> getCardsByBoard(Long boardId, Long userId) {
        List<Card> cards = cardRepo.findByBoardIdAndIsArchivedFalse(boardId);
        return cards.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public List<CardResponse> getCardsByAssignee(Long assigneeId, Long userId) {
        List<Card> cards = cardRepo.findByAssigneeId(assigneeId);
        return cards.stream()
                .filter(c -> !c.getIsArchived())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<CardResponse> getOverdueCards(Long boardId, Long userId) {
        List<Card> cards = cardRepo.findByDueDateBeforeAndStatusNot(LocalDate.now(), Status.DONE);
        return cards.stream()
                .filter(c -> c.getBoardId().equals(boardId) && !c.getIsArchived())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getTotalCount() {
        return cardRepo.count();
    }

    public List<CardResponse> getAllOverdueCards() {
        List<Card> overdue = cardRepo.findByDueDateBeforeAndStatusNot(LocalDate.now(), Status.DONE);
        return overdue.stream()
                .filter(c -> !c.getIsArchived())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private void logActivity(Long cardId, Long actorId, String actorName, String action, String details) {
        CardActivity activity = new CardActivity();
        activity.setCardId(cardId);
        activity.setActorId(actorId);
        activity.setActorName(actorName);
        activity.setAction(action);
        activity.setDetails(details);
        activityRepo.save(activity);
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .listId(card.getListId())
                .boardId(card.getBoardId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .priority(card.getPriority())
                .status(card.getStatus())
                .dueDate(card.getDueDate())
                .startDate(card.getStartDate())
                .assigneeId(card.getAssigneeId())
                .createdBy(card.getCreatedBy())
                .coverColor(card.getCoverColor())
                .isArchived(card.getIsArchived())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
