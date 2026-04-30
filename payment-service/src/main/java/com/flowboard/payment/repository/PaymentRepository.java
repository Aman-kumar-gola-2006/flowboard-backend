package com.flowboard.payment.repository;

import com.flowboard.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String orderId);
    Optional<Payment> findByWorkspaceIdAndStatus(Long workspaceId, String status);
}
