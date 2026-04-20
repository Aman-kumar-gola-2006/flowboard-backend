package com.flowboard.label.service;

import com.flowboard.label.client.BoardClient;
import com.flowboard.label.model.CardLabel;
import com.flowboard.label.model.Label;
import com.flowboard.label.repository.CardLabelRepository;
import com.flowboard.label.repository.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabelService {
    
    private static final Logger log = LoggerFactory.getLogger(LabelService.class);
    
    @Autowired
    private LabelRepository labelRepo;
    
    @Autowired
    private CardLabelRepository cardLabelRepo;
    
    @Autowired
    private BoardClient boardClient;
    
    /**
     * Get all labels for a board
     */
    public List<Label> getBoardLabels(Long boardId, Long userId) {
        log.info("Fetching labels for board {}", boardId);
        
        // Check board access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Board not found or access denied");
        }
        
        return labelRepo.findByBoardId(boardId);
    }
    
    /**
     * Create a new label for a board
     */
    @Transactional
    public Label createLabel(Long boardId, String name, String color, Long userId) {
        log.info("Creating label '{}' for board {}", name, boardId);
        
        // Check board access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Board not found or access denied");
        }
        
        // Check if label already exists
        if (labelRepo.existsByBoardIdAndName(boardId, name)) {
            throw new RuntimeException("Label '" + name + "' already exists in this board");
        }
        
        Label label = new Label();
        label.setBoardId(boardId);
        label.setName(name);
        label.setColor(color != null ? color : "#cccccc");
        
        Label saved = labelRepo.save(label);
        log.info("Label created with ID: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Update a label
     */
    @Transactional
    public Label updateLabel(Long labelId, String name, String color, Long userId) {
        Label label = labelRepo.findById(labelId)
                .orElseThrow(() -> new RuntimeException("Label not found"));
        
        // Check board access
        try {
            boardClient.getBoardById(label.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        if (name != null) {
            label.setName(name);
        }
        if (color != null) {
            label.setColor(color);
        }
        
        return labelRepo.save(label);
    }
    
    /**
     * Delete a label (also removes from all cards)
     */
    @Transactional
    public void deleteLabel(Long labelId, Long userId) {
        Label label = labelRepo.findById(labelId)
                .orElseThrow(() -> new RuntimeException("Label not found"));
        
        // Check board access
        try {
            boardClient.getBoardById(label.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete all card-label associations first
        cardLabelRepo.deleteByLabelId(labelId);
        
        // Delete the label
        labelRepo.deleteById(labelId);
        log.info("Label {} deleted", labelId);
    }
    
    /**
     * Add a label to a card
     */
    @Transactional
    public void addLabelToCard(Long cardId, Long labelId, Long boardId, Long userId) {
        log.info("Adding label {} to card {}", labelId, cardId);
        
        // Check board access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        // Verify label belongs to this board
        Label label = labelRepo.findById(labelId)
                .orElseThrow(() -> new RuntimeException("Label not found"));
        
        if (!label.getBoardId().equals(boardId)) {
            throw new RuntimeException("Label does not belong to this board");
        }
        
        // Check if already added
        if (cardLabelRepo.existsByCardIdAndLabelId(cardId, labelId)) {
            log.info("Label already on card, skipping");
            return;
        }
        
        CardLabel cardLabel = new CardLabel();
        cardLabel.setCardId(cardId);
        cardLabel.setLabelId(labelId);
        cardLabelRepo.save(cardLabel);
    }
    
    /**
     * Remove a label from a card
     */
    @Transactional
    public void removeLabelFromCard(Long cardId, Long labelId, Long boardId, Long userId) {
        log.info("Removing label {} from card {}", labelId, cardId);
        
        // Check board access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        cardLabelRepo.deleteByCardIdAndLabelId(cardId, labelId);
    }
    
    /**
     * Get all labels on a specific card
     */
    public List<Label> getCardLabels(Long cardId, Long boardId, Long userId) {
        log.info("Fetching labels for card {}", cardId);
        
        // Check board access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        List<CardLabel> cardLabels = cardLabelRepo.findByCardId(cardId);
        List<Long> labelIds = cardLabels.stream()
                .map(CardLabel::getLabelId)
                .collect(Collectors.toList());
        
        return labelRepo.findAllById(labelIds);
    }
}
