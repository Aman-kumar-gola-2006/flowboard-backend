package com.flowboard.card.dto;

import lombok.Data;

@Data
public class MoveCardRequest {
    private Long targetListId;
    private Integer newPosition;
}
