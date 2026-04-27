package com.flowboard.auth.controller;

import com.flowboard.auth.dto.MessageResponse;
import com.flowboard.auth.dto.UserResponse;
import com.flowboard.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private com.flowboard.auth.service.AuditLogService auditLogService;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/overdue-cards")
    public ResponseEntity<?> getOverdueCards() {
        try {
            Object overdueCards = restTemplate.getForObject(
                "http://localhost:8085/api/cards/overdue/all", Object.class);
            return ResponseEntity.ok(overdueCards);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("message", "Card service unavailable", "count", 0));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<com.flowboard.auth.model.AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<MessageResponse> suspendUser(@PathVariable Long id) {
        authService.suspendUser(id);
        return ResponseEntity.ok(new MessageResponse("User suspended", true));
    }

    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<MessageResponse> reactivateUser(@PathVariable Long id) {
        authService.reactivateUser(id);
        return ResponseEntity.ok(new MessageResponse("User reactivated", true));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted", true));
    }
}
