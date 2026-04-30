package com.flowboard.payment.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Long workspaceId;
    private Long userId;
    private Integer amount; // in rupees
}
