package com.flowboard.card.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveCardRequest {
    private Long targetListId;
    private Integer newPosition;
}
