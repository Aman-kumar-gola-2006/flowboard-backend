package com.flowboard.notification.service;

import com.flowboard.notification.client.AuthClient;
import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.dto.NotificationResponse;
import com.flowboard.notification.model.Notification;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationWebSocketHandler webSocketHandler;
    @Mock private AuthClient authClient;
    @Mock private RestTemplate restTemplate;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private NotificationRequest testRequest;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setRecipientId(1L);
        testNotification.setActorId(2L);
        testNotification.setType("COMMENT");
        testNotification.setTitle("New Comment");
        testNotification.setMessage("Someone commented on your card");
        testNotification.setRelatedId(100L);
        testNotification.setRelatedType("CARD");
        testNotification.setIsRead(false);
        testNotification.setIsEmailSent(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        testRequest = new NotificationRequest();
        testRequest.setRecipientId(1L);
        testRequest.setActorId(2L);
        testRequest.setType("COMMENT");
        testRequest.setTitle("New Comment");
        testRequest.setMessage("Someone commented on your card");
        testRequest.setRelatedId(100L);
        testRequest.setRelatedType("CARD");
    }

    // ========== CREATE NOTIFICATION ==========

    @Test
    @DisplayName("CreateNotification - saves notification and sends WebSocket")
    void createNotification_ShouldSaveAndSendWebSocket() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(webSocketHandler).sendNotification(any(), any());

        Notification result = notificationService.createNotification(testRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertFalse(result.getIsRead());
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketHandler).sendNotification(eq(1L), any());
    }

    // ========== SEND NOTIFICATION ==========

    @Test
    @DisplayName("SendNotification - creates and returns notification response")
    void sendNotification_ShouldReturnNotificationResponse() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(webSocketHandler).sendNotification(any(), any());

        NotificationResponse result = notificationService.sendNotification(testRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("COMMENT", result.getType());
        assertEquals("New Comment", result.getTitle());
        assertFalse(result.getIsRead());
    }

    // ========== GET USER NOTIFICATIONS ==========

    @Test
    @DisplayName("GetUserNotifications - returns notifications for user")
    void getUserNotifications_ShouldReturnNotifications() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testNotification));

        List<NotificationResponse> results = notificationService.getUserNotifications(1L);

        assertThat(results).hasSize(1);
        assertEquals("New Comment", results.get(0).getTitle());
    }

    @Test
    @DisplayName("GetUserNotifications - returns empty list when no notifications")
    void getUserNotifications_WhenNone_ShouldReturnEmptyList() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<NotificationResponse> results = notificationService.getUserNotifications(1L);

        assertThat(results).isEmpty();
    }

    // ========== GET UNREAD COUNT ==========

    @Test
    @DisplayName("GetUnreadCount - returns correct unread count")
    void getUnreadCount_ShouldReturnCorrectCount() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(5L);

        Long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    // ========== MARK AS READ ==========

    @Test
    @DisplayName("MarkAsRead - sets isRead to true for existing notification")
    void markAsRead_WhenExists_ShouldSetReadTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        assertDoesNotThrow(() -> notificationService.markAsRead(1L));
        assertTrue(testNotification.getIsRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("MarkAsRead - does nothing when notification not found")
    void markAsRead_WhenNotFound_ShouldDoNothing() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.markAsRead(999L));
        verify(notificationRepository, never()).save(any());
    }

    // ========== MARK ALL AS READ ==========

    @Test
    @DisplayName("MarkAllAsRead - marks all unread notifications as read")
    void markAllAsRead_ShouldMarkAllRead() {
        Notification n2 = new Notification();
        n2.setId(2L);
        n2.setRecipientId(1L);
        n2.setIsRead(false);

        when(notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testNotification, n2));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        assertDoesNotThrow(() -> notificationService.markAllAsRead(1L));
        assertTrue(testNotification.getIsRead());
        assertTrue(n2.getIsRead());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    // ========== CLEAR READ NOTIFICATIONS ==========

    @Test
    @DisplayName("ClearReadNotifications - deletes all read notifications for user")
    void clearReadNotifications_ShouldCallDelete() {
        doNothing().when(notificationRepository).deleteByRecipientIdAndIsReadTrue(1L);

        assertDoesNotThrow(() -> notificationService.clearReadNotifications(1L));
        verify(notificationRepository).deleteByRecipientIdAndIsReadTrue(1L);
    }

    // ========== GET ALL FOR ADMIN ==========

    @Test
    @DisplayName("GetAllNotificationsForAdmin - returns all notifications")
    void getAllNotificationsForAdmin_ShouldReturnAll() {
        when(notificationRepository.findAll()).thenReturn(List.of(testNotification));

        List<Notification> results = notificationService.getAllNotificationsForAdmin();

        assertThat(results).hasSize(1);
    }
}
