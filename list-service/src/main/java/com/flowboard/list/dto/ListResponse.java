package com.flowboard.list.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListResponse {
    private Long id;
    private Long boardId;
    private String name;
    private Integer position;
    private String color;
    private Boolean isArchived;
    private LocalDateTime createdAt;
    private Integer cardCount; // Will be populated later
}
