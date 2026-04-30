package com.flowboard.auth.service;

import com.flowboard.auth.dto.*;
import com.flowboard.auth.model.User;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.security.JwtUtil;
import com.flowboard.auth.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private RestTemplate restTemplate;
    @Mock private AuditLogService auditLogService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@flowboard.com");
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setFullName("Test User");
        testUser.setRole("MEMBER");
        testUser.setIsActive(true);
        testUser.setIsPro(false);
        testUser.setEmailVerified(false);
        testUser.setProvider("LOCAL");
    }

    @Test
    @DisplayName("Register - success with new credentials")
    void register_WithNewEmailAndUsername_ShouldReturnSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setEmail("new@flowboard.com");
        request.setUsername("newuser");
        request.setPassword("password123");

        when(userRepository.existsByEmail("new@flowboard.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        MessageResponse result = authService.register(request);

        assertTrue(result.isSuccess());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - fail when email already exists")
    void register_WithExistingEmail_ShouldReturnFailure() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@flowboard.com");
        request.setUsername("newuser");

        when(userRepository.existsByEmail("test@flowboard.com")).thenReturn(true);

        MessageResponse result = authService.register(request);

        assertFalse(result.isSuccess());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register - fail when username already taken")
    void register_WithExistingUsername_ShouldReturnFailure() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("unique@flowboard.com");
        request.setUsername("testuser");

        when(userRepository.existsByEmail("unique@flowboard.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        MessageResponse result = authService.register(request);

        assertFalse(result.isSuccess());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("GetUserById - returns user when found")
    void getUserById_WithValidId_ShouldReturnUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = authService.getUserById(1L);

        assertNotNull(result);
        assertEquals("test@flowboard.com", result.getEmail());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("GetUserById - throws exception when not found")
    void getUserById_WithInvalidId_ShouldThrowRuntimeException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getUserById(999L));
    }

    @Test
    @DisplayName("GetUserByEmail - returns user when found")
    void getUserByEmail_WithValidEmail_ShouldReturnUserResponse() {
        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));

        UserResponse result = authService.getUserByEmail("test@flowboard.com");

        assertNotNull(result);
        assertEquals("test@flowboard.com", result.getEmail());
    }

    @Test
    @DisplayName("GetUserByEmail - throws exception when not found")
    void getUserByEmail_WithInvalidEmail_ShouldThrowRuntimeException() {
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getUserByEmail("notfound@test.com"));
    }

    @Test
    @DisplayName("SearchUsers - returns matching users")
    void searchUsers_WithValidName_ShouldReturnMatchingUsers() {
        when(userRepository.searchByFullName("Test")).thenReturn(List.of(testUser));

        List<UserResponse> results = authService.searchUsers("Test");

        assertThat(results).hasSize(1);
        assertEquals("Test User", results.get(0).getFullName());
    }

    @Test
    @DisplayName("UpdateProfile - success updating full name")
    void updateProfile_WithNewFullName_ShouldUpdateSuccessfully() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        MessageResponse result = authService.updateProfile(1L, request);

        assertTrue(result.isSuccess());
        assertEquals("Updated Name", testUser.getFullName());
    }

    @Test
    @DisplayName("UpdateProfile - fail when current password wrong")
    void updateProfile_WithWrongCurrentPassword_ShouldReturnFailure() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        MessageResponse result = authService.updateProfile(1L, request);

        assertFalse(result.isSuccess());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("DeactivateAccount - sets user inactive")
    void deactivateAccount_ShouldSetUserInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        MessageResponse result = authService.deactivateAccount(1L);

        assertTrue(result.isSuccess());
        assertFalse(testUser.getIsActive());
    }

    @Test
    @DisplayName("ReactivateAccount - sets user active")
    void reactivateAccount_ShouldSetUserActive() {
        testUser.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        MessageResponse result = authService.reactivateAccount(1L);

        assertTrue(result.isSuccess());
        assertTrue(testUser.getIsActive());
    }

    @Test
    @DisplayName("CreatePasswordResetOtp - generates OTP and sends email")
    void createPasswordResetOtp_WithValidEmail_ShouldSendOtp() {
        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.createPasswordResetOtp("test@flowboard.com"));

        verify(emailService).sendOtpEmail(eq("test@flowboard.com"), anyString());
        assertNotNull(testUser.getResetToken());
        assertThat(testUser.getResetToken()).hasSize(6);
    }

    @Test
    @DisplayName("VerifyOtp - success with valid OTP")
    void verifyOtp_WithValidOtp_ShouldSetOtpVerified() {
        testUser.setResetToken("123456");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));
        testUser.setIsOtpVerified(false);

        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.verifyOtp("test@flowboard.com", "123456"));
        assertTrue(testUser.getIsOtpVerified());
    }

    @Test
    @DisplayName("VerifyOtp - throws exception with invalid OTP")
    void verifyOtp_WithInvalidOtp_ShouldThrowException() {
        testUser.setResetToken("123456");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class,
                () -> authService.verifyOtp("test@flowboard.com", "000000"));
    }

    @Test
    @DisplayName("VerifyOtp - throws exception when OTP expired")
    void verifyOtp_WithExpiredOtp_ShouldThrowException() {
        testUser.setResetToken("123456");
        testUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp("test@flowboard.com", "123456"));
        assertThat(ex.getMessage()).containsIgnoringCase("expired");
    }

    @Test
    @DisplayName("ResetPasswordWithOtp - success when OTP verified")
    void resetPasswordWithOtp_WhenOtpVerified_ShouldResetPassword() {
        testUser.setIsOtpVerified(true);
        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        MessageResponse result = authService.resetPasswordWithOtp("test@flowboard.com", "newPassword");

        assertTrue(result.isSuccess());
        assertNull(testUser.getResetToken());
        assertFalse(testUser.getIsOtpVerified());
    }

    @Test
    @DisplayName("ResetPasswordWithOtp - throws when OTP not verified")
    void resetPasswordWithOtp_WhenOtpNotVerified_ShouldThrowException() {
        testUser.setIsOtpVerified(false);
        when(userRepository.findByEmail("test@flowboard.com")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class,
                () -> authService.resetPasswordWithOtp("test@flowboard.com", "newPassword"));
    }

    @Test
    @DisplayName("UpgradeUser - sets user to PRO")
    void upgradeUser_ShouldSetUserToPro() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any());

        MessageResponse result = authService.upgradeUser(1L);

        assertTrue(result.isSuccess());
        assertTrue(testUser.getIsPro());
    }

    @Test
    @DisplayName("DeleteUser - calls repository delete")
    void deleteUser_ShouldDeleteAndReturnSuccess() {
        doNothing().when(userRepository).deleteById(1L);
        doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any());

        MessageResponse result = authService.deleteUser(1L);

        assertTrue(result.isSuccess());
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("ValidateToken - returns true for valid token")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        when(jwtUtil.validateToken("validToken")).thenReturn(true);
        assertTrue(authService.validateToken("validToken"));
    }

    @Test
    @DisplayName("GetAllUsers - returns all users")
    void getAllUsers_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<UserResponse> result = authService.getAllUsers();
        assertThat(result).hasSize(1);
    }
}
