package com.flowboard.board.controller;

import com.flowboard.board.model.CardLabel;
import com.flowboard.board.model.Label;
import com.flowboard.board.repository.CardLabelRepository;
import com.flowboard.board.repository.LabelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/labels")
@CrossOrigin(origins = "*")
public class LabelController {

    @Autowired
    private LabelRepository labelRepo;

    @Autowired
    private CardLabelRepository cardLabelRepo;

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Label>> getBoardLabels(@PathVariable Long boardId) {
        return ResponseEntity.ok(labelRepo.findByBoardId(boardId));
    }

    @PostMapping("/board/{boardId}")
    public ResponseEntity<Label> createLabel(@PathVariable Long boardId, @RequestBody Map<String, String> payload) {
        Label label = new Label();
        label.setBoardId(boardId);
        label.setName(payload.get("name"));
        label.setColor(payload.get("color"));
        return ResponseEntity.ok(labelRepo.save(label));
    }

    @PostMapping("/card/{cardId}/add/{labelId}")
    public ResponseEntity<?> addLabelToCard(@PathVariable Long cardId, @PathVariable Long labelId, @RequestParam Long boardId) {
        if (cardLabelRepo.findByCardIdAndLabelId(cardId, labelId).isEmpty()) {
            CardLabel cardLabel = new CardLabel();
            cardLabel.setCardId(cardId);
            cardLabel.setLabelId(labelId);
            cardLabel.setBoardId(boardId);
            cardLabelRepo.save(cardLabel);
        }
        return ResponseEntity.ok(Map.of("message", "Label added to card"));
    }

    @DeleteMapping("/card/{cardId}/remove/{labelId}")
    @Transactional
    public ResponseEntity<?> removeLabelFromCard(@PathVariable Long cardId, @PathVariable Long labelId, @RequestParam Long boardId) {
        cardLabelRepo.deleteByCardIdAndLabelId(cardId, labelId);
        return ResponseEntity.ok(Map.of("message", "Label removed from card"));
    }
    
    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<Label>> getCardLabels(@PathVariable Long cardId) {
        List<Long> labelIds = cardLabelRepo.findByCardId(cardId).stream()
                .map(CardLabel::getLabelId)
                .toList();
        return ResponseEntity.ok(labelRepo.findAllById(labelIds));
    }
}
