package com.flowboard.card.dto;

import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CardRequest {
    private Long listId;
    private Long boardId;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private String coverColor;
}
