package com.flowboard.board.service;

import com.flowboard.board.model.Label;
import com.flowboard.board.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    public List<Label> getLabelsByBoard(Long boardId) {
        return labelRepository.findByBoardId(boardId);
    }

    public Label createLabel(Long boardId, String name, String color) {
        Label label = new Label();
        label.setBoardId(boardId);
        label.setName(name);
        label.setColor(color);
        return labelRepository.save(label);
    }

    public Label getLabelById(Long id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Label not found"));
    }

    public void deleteLabel(Long id) {
        labelRepository.deleteById(id);
    }

    public List<Label> findAllById(List<Long> ids) {
        return labelRepository.findAllById(ids);
    }
}
