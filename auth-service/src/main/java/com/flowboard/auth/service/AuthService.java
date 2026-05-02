package com.flowboard.auth.service;

import com.flowboard.auth.dto.*;
import com.flowboard.auth.dto.NotificationMessage;
import com.flowboard.auth.model.User;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.security.JwtUtil;
import com.flowboard.auth.security.UserDetailsImpl;
import com.flowboard.auth.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MessageProducer messageProducer;

    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmailOrUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.getIsActive()) {
            throw new RuntimeException("Your account has been suspended by an admin. Please contact support.");
        }

        auditLogService.log(userDetails.getId(), userDetails.getFullName(), "LOGIN", "USER", userDetails.getId(), "User logged in successfully");

        return new JwtResponse(jwt, "Bearer", userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(),
                userDetails.getFullName(), userDetails.getRole(), userDetails.getIsPro());
    }

    public MessageResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new MessageResponse("Error: Email is already in use!", false);
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new MessageResponse("Error: Username is already taken!", false);
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setRole("MEMBER");
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setProvider("LOCAL");

        userRepository.save(user);
        
        // Send Welcome Email via RabbitMQ (Async)
        try {
            messageProducer.sendNotification(new NotificationMessage(user.getEmail(), user.getFullName(), "WELCOME", null));
        } catch (Exception e) {
            System.err.println("Failed to queue welcome email: " + e.getMessage());
        }

        return new MessageResponse("User registered successfully!", true);
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToUserResponse(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return mapToUserResponse(user);
    }

    public List<UserResponse> searchUsers(String name) {
        List<User> users = userRepository.searchByFullName(name);
        return users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    public MessageResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return new MessageResponse("Current password is incorrect", false);
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return new MessageResponse("Profile updated successfully", true);
    }

    public MessageResponse deactivateAccount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        return new MessageResponse("Account deactivated successfully", true);
    }

    public MessageResponse reactivateAccount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);
        return new MessageResponse("Account reactivated successfully", true);
    }

    public void suspendUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        
        // Send Suspension Email via RabbitMQ (Async)
        try {
            messageProducer.sendNotification(new NotificationMessage(user.getEmail(), user.getFullName(), "SUSPEND", null));
        } catch (Exception e) {
            System.err.println("Failed to queue suspension email: " + e.getMessage());
        }

        auditLogService.log(null, "ADMIN", "SUSPEND", "USER", id, "Suspended user: " + user.getEmail());
    }

    public void reactivateUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);

        // Send Reactivation Email via RabbitMQ (Async)
        try {
            messageProducer.sendNotification(new NotificationMessage(user.getEmail(), user.getFullName(), "REACTIVATE", null));
        } catch (Exception e) {
            System.err.println("Failed to queue reactivation email: " + e.getMessage());
        }

        auditLogService.log(null, "ADMIN", "REACTIVATE", "USER", id, "Reactivated user: " + user.getEmail());
    }

    public MessageResponse deleteUser(Long userId) {
        userRepository.deleteById(userId);
        auditLogService.log(null, "ADMIN", "DELETE", "USER", userId, "Permanently deleted user with ID: " + userId);
        return new MessageResponse("User deleted successfully", true);
    }

    public Boolean validateToken(String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                return userRepository.existsByUsername(username) && 
                       userRepository.findByUsername(username).map(User::getIsActive).orElse(false);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public String refreshToken(String oldToken) {
        if (jwtUtil.validateToken(oldToken)) {
            String username = jwtUtil.extractUsername(oldToken);
            UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
            return jwtUtil.generateToken(userDetails);
        }
        throw new RuntimeException("Invalid token");
    }

    public Map<String, Long> getAdminStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        
        try {
            Long workspaces = restTemplate.getForObject("http://localhost:8082/api/workspaces/count", Long.class);
            stats.put("totalWorkspaces", workspaces != null ? workspaces : 0L);
        } catch (Exception e) {
            stats.put("totalWorkspaces", 0L);
        }

        try {
            Long boards = restTemplate.getForObject("http://localhost:8083/api/boards/count", Long.class);
            stats.put("totalBoards", boards != null ? boards : 0L);
        } catch (Exception e) {
            stats.put("totalBoards", 0L);
        }

        try {
            Long cards = restTemplate.getForObject("http://localhost:8085/api/cards/count", Long.class);
            stats.put("totalCards", cards != null ? cards : 0L);
        } catch (Exception e) {
            stats.put("totalCards", 0L);
        }
        
        return stats;
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder().id(user.getId()).email(user.getEmail()).username(user.getUsername())
                .fullName(user.getFullName()).avatarUrl(user.getAvatarUrl()).role(user.getRole())
                .isActive(user.getIsActive()).isPro(user.getIsPro()).createdAt(user.getCreatedAt()).build();
    }

    public void createPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setResetToken(otp);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        user.setIsOtpVerified(false);
        userRepository.save(user);
        
        // Send OTP Email via RabbitMQ (Async)
        messageProducer.sendNotification(new NotificationMessage(email, "User", "OTP", otp));
    }

    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetToken() == null || !user.getResetToken().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        user.setIsOtpVerified(true);
        userRepository.save(user);
    }

    public MessageResponse resetPasswordWithOtp(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsOtpVerified()) {
            throw new RuntimeException("OTP not verified");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setIsOtpVerified(false);
        userRepository.save(user);

        return new MessageResponse("Password reset successfully", true);
    }

    public MessageResponse upgradeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsPro(true);
        userRepository.save(user);
        
        // Send Pro Upgrade Email via RabbitMQ (Async)
        try {
            messageProducer.sendNotification(new NotificationMessage(user.getEmail(), user.getFullName(), "PRO", null));
        } catch (Exception e) {
            System.err.println("Failed to queue PRO upgrade email: " + e.getMessage());
        }

        auditLogService.log(userId, user.getFullName(), "UPGRADE", "USER", userId, "User upgraded to PRO");
        return new MessageResponse("User upgraded to PRO successfully", true);
    }
}
