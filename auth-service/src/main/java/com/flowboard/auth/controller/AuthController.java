package com.flowboard.auth.controller;

import com.flowboard.auth.dto.LoginRequest;
import com.flowboard.auth.dto.RegisterRequest;
import com.flowboard.auth.dto.JwtResponse;
import com.flowboard.auth.dto.MessageResponse;
import com.flowboard.auth.dto.UserResponse;
import com.flowboard.auth.dto.UpdateProfileRequest;
import com.flowboard.auth.dto.SupportRequest;
import com.flowboard.auth.service.AuthService;
import com.flowboard.auth.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.login(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        MessageResponse response = authService.register(registerRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok(new MessageResponse("Logged out successfully", true));
    }
    
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        try {
            UserResponse userResponse = authService.getCurrentUser();
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @GetMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserResponse userResponse = authService.getUserById(id);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @GetMapping("/users/email/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            UserResponse userResponse = authService.getUserByEmail(email);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @GetMapping("/users/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchUsers(@RequestParam String name) {
        try {
            List<UserResponse> users = authService.searchUsers(name);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UserResponse> users = authService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            MessageResponse response = authService.updateProfile(currentUser.getId(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            MessageResponse response = authService.deactivateAccount(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @PutMapping("/users/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id) {
        try {
            MessageResponse response = authService.reactivateAccount(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    @PutMapping("/users/{id}/upgrade")
    public ResponseEntity<?> upgradeUser(@PathVariable Long id) {
        try {
            MessageResponse response = authService.upgradeUser(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            MessageResponse response = authService.deleteUser(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminStats() {
        try {
            Map<String, Long> stats = authService.getAdminStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(new MessageResponse(isValid ? "Token is valid" : "Token is invalid", isValid));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String token) {
        try {
            String newToken = authService.refreshToken(token);
            return ResponseEntity.ok(new JwtResponse(newToken, "Bearer", null, null, null, null, null, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    @Autowired
    private EmailService emailService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        try {
            authService.createPasswordResetOtp(email);
            return ResponseEntity.ok(new MessageResponse("OTP sent to your email", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        try {
            authService.verifyOtp(email, otp);
            return ResponseEntity.ok(new MessageResponse("OTP verified successfully", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        String newPassword = payload.get("newPassword");
        try {
            MessageResponse response = authService.resetPasswordWithOtp(email, newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }

    @PostMapping("/contact")
    public ResponseEntity<?> contactSupport(@Valid @RequestBody SupportRequest request) {
        try {
            emailService.sendSupportEmail(request.getName(), request.getEmail(), request.getSubject(), request.getMessage());
            return ResponseEntity.ok(new MessageResponse("Your message has been sent to the admin. We will get back to you soon.", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to send message: " + e.getMessage(), false));
        }
    }
}
