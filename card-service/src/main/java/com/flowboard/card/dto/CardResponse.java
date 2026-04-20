package com.flowboard.card.dto;

import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private Long listId;
    private Long boardId;
    private String title;
    private String description;
    private Integer position;
    private Priority priority;
    private Status status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private Long createdBy;
    private String coverColor;
    private Boolean isArchived;
    private LocalDateTime createdAt;
}
