package com.flowboard.board.dto;

import lombok.Data;

@Data
public class BoardRequest {
    private Long workspaceId;
    private String name;
    private String description;
    private String backgroundColor;
    private String visibility;
}
