package com.flowboard.card.dto;

import com.flowboard.card.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    private Long listId;
    private Long boardId;
    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private String coverColor;
}
