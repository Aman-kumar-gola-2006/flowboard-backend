package com.flowboard.payment.service;

import com.flowboard.payment.client.WorkspaceClient;
import com.flowboard.payment.client.AuthClient;
import com.flowboard.payment.dto.OrderRequest;
import com.flowboard.payment.dto.OrderResponse;
import com.flowboard.payment.dto.VerifyRequest;
import com.flowboard.payment.model.Payment;
import com.flowboard.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {
    
    @Value("${razorpay.key-id}")
    private String keyId;
    
    @Value("${razorpay.key-secret}")
    private String keySecret;
    
    @Autowired
    private PaymentRepository paymentRepo;
    
    @Autowired
    private WorkspaceClient workspaceClient;

    @Autowired
    private AuthClient authClient;
    
    public OrderResponse createOrder(OrderRequest request) throws Exception {
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount() * 100); // paise mein
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + request.getWorkspaceId());
        
        Order order = razorpay.orders.create(orderRequest);
        
        // Save to DB
        Payment payment = new Payment();
        payment.setWorkspaceId(request.getWorkspaceId());
        payment.setUserId(request.getUserId());
        payment.setRazorpayOrderId(order.get("id"));
        payment.setAmount(request.getAmount());
        payment.setStatus("CREATED");
        paymentRepo.save(payment);
        
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.get("id"));
        response.setAmount(request.getAmount());
        response.setCurrency("INR");
        response.setKeyId(keyId);
        
        return response;
    }
    
    public Map<String, Object> verifyPayment(VerifyRequest request) throws Exception {
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", request.getRazorpayOrderId());
        attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
        attributes.put("razorpay_signature", request.getRazorpaySignature());
        
        boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);
        
        Map<String, Object> result = new HashMap<>();
        
        if (isValid) {
            // Update payment status
            Payment payment = paymentRepo.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus("PAID");
            paymentRepo.save(payment);
            
            // Update workspace to PRO
            workspaceClient.upgradeWorkspace(request.getWorkspaceId());

            // Update user to PRO
            authClient.upgradeUser(payment.getUserId());
            
            result.put("success", true);
            result.put("message", "Payment verified! Workspace upgraded to PRO!");
        } else {
            result.put("success", false);
            result.put("message", "Payment verification failed!");
        }
        
        return result;
    }
}
