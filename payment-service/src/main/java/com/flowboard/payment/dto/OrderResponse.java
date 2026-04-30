package com.flowboard.payment.dto;

import lombok.Data;

@Data
public class OrderResponse {
    private String orderId;
    private Integer amount;
    private String currency;
    private String keyId;
}
