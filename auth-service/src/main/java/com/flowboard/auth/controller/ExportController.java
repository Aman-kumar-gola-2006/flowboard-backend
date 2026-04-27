package com.flowboard.auth.controller;

import com.flowboard.auth.model.AuditLog;
import com.flowboard.auth.service.AuditLogService;
import com.flowboard.auth.service.AuthService;
import com.flowboard.auth.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/export")
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuthService authService;

    @GetMapping("/audit-logs/csv")
    public ResponseEntity<byte[]> exportAuditLogsCSV() {
        List<AuditLog> logs = auditLogService.getAllLogs();

        StringBuilder csv = new StringBuilder();
        csv.append("Time,Actor,Action,Entity Type,Entity ID,Details\n");

        for (AuditLog log : logs) {
            csv.append(log.getCreatedAt()).append(",");
            csv.append(escape(log.getActorName())).append(",");
            csv.append(log.getAction()).append(",");
            csv.append(log.getEntityType()).append(",");
            csv.append(log.getEntityId()).append(",");
            csv.append(escape(log.getDetails())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit-logs.csv");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/users/csv")
    public ResponseEntity<byte[]> exportUsersCSV() {
        List<UserResponse> users = authService.getAllUsers();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Full Name,Email,Username,Role,Active\n");

        for (UserResponse user : users) {
            csv.append(user.getId()).append(",");
            csv.append(escape(user.getFullName())).append(",");
            csv.append(escape(user.getEmail())).append(",");
            csv.append(escape(user.getUsername())).append(",");
            csv.append(user.getRole()).append(",");
            csv.append(user.getIsActive()).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "users.csv");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
