package com.flowboard.card.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveCardRequest {
    private Long targetListId;
    private Integer newPosition;
}
