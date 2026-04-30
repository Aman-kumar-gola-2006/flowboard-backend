package com.flowboard.board.service;

import com.flowboard.board.model.Label;
import com.flowboard.board.repository.LabelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LabelService Unit Tests")
class LabelServiceTest {

    @Mock
    private LabelRepository labelRepository;

    @InjectMocks
    private LabelService labelService;

    private Label testLabel;

    @BeforeEach
    void setUp() {
        testLabel = new Label();
        testLabel.setId(1L);
        testLabel.setBoardId(1L);
        testLabel.setName("Bug");
        testLabel.setColor("#FF0000");
    }

    @Test
    @DisplayName("GetLabelsByBoard - returns labels for a board")
    void getLabelsByBoard_ShouldReturnLabels() {
        when(labelRepository.findByBoardId(1L)).thenReturn(List.of(testLabel));

        List<Label> results = labelService.getLabelsByBoard(1L);

        assertThat(results).hasSize(1);
        assertEquals("Bug", results.get(0).getName());
    }

    @Test
    @DisplayName("GetLabelsByBoard - returns empty list when no labels")
    void getLabelsByBoard_WhenNoLabels_ShouldReturnEmptyList() {
        when(labelRepository.findByBoardId(99L)).thenReturn(List.of());

        List<Label> results = labelService.getLabelsByBoard(99L);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("CreateLabel - saves and returns label")
    void createLabel_ShouldSaveAndReturnLabel() {
        when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

        Label result = labelService.createLabel(1L, "Bug", "#FF0000");

        assertNotNull(result);
        assertEquals("Bug", result.getName());
        assertEquals("#FF0000", result.getColor());
        verify(labelRepository).save(any(Label.class));
    }

    @Test
    @DisplayName("GetLabelById - returns label when found")
    void getLabelById_WithValidId_ShouldReturnLabel() {
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));

        Label result = labelService.getLabelById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Bug", result.getName());
    }

    @Test
    @DisplayName("GetLabelById - throws exception when not found")
    void getLabelById_WithInvalidId_ShouldThrowException() {
        when(labelRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> labelService.getLabelById(999L));
        assertThat(ex.getMessage()).containsIgnoringCase("not found");
    }

    @Test
    @DisplayName("DeleteLabel - calls repository deleteById")
    void deleteLabel_ShouldCallDeleteById() {
        doNothing().when(labelRepository).deleteById(1L);

        assertDoesNotThrow(() -> labelService.deleteLabel(1L));
        verify(labelRepository).deleteById(1L);
    }

    @Test
    @DisplayName("FindAllById - returns labels for given IDs")
    void findAllById_ShouldReturnMatchingLabels() {
        when(labelRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(testLabel));

        List<Label> results = labelService.findAllById(List.of(1L, 2L));

        assertThat(results).hasSize(1);
    }
}
