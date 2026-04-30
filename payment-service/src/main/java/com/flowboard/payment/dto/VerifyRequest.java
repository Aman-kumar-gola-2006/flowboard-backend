package com.flowboard.payment.dto;

import lombok.Data;

@Data
public class VerifyRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private Long workspaceId;
}
