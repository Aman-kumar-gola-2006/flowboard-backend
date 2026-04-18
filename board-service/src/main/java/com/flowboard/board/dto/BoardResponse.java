package com.flowboard.board.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BoardResponse {
    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String backgroundColor;
    private String visibility;
    private Long createdBy;
    private Boolean isClosed;
    private LocalDateTime createdAt;
    private Integer memberCount;
}
