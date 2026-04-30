package com.flowboard.payment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "workspace_id")
    private Long workspaceId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;
    
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;
    
    @Column(name = "razorpay_signature")
    private String razorpaySignature;
    
    @Column(name = "amount")
    private Integer amount;
    
    @Column(name = "status")
    private String status; // CREATED, PAID, FAILED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
