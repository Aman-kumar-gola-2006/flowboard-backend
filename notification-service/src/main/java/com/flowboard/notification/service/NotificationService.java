package com.flowboard.notification.service;

import com.flowboard.notification.client.AuthClient;
import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.dto.NotificationResponse;
import com.flowboard.notification.event.NotificationEvent;
import com.flowboard.notification.model.Notification;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final AuthClient authClient;

    @Transactional
    public Notification createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setRecipientId(request.getRecipientId());
        notification.setActorId(request.getActorId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRelatedId(request.getRelatedId());
        notification.setRelatedType(request.getRelatedType());
        notification.setDeepLink(request.getDeepLink());
        notification.setIsRead(false);
        notification.setIsEmailSent(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        
        // Send WebSocket notification
        sendWebSocketNotification(saved);
        
        log.info("Notification created: {} for user {}", saved.getId(), saved.getRecipientId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void clearReadNotifications(Long userId) {
        notificationRepository.deleteByRecipientIdAndIsReadTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotificationsForAdmin() {
        return notificationRepository.findAll();
    }

    @Transactional
    public void sendWebSocketNotification(Notification notification) {
        try {
            Map<String, Object> payload = Map.of(
                "id", notification.getId(),
                "type", notification.getType(),
                "title", notification.getTitle(),
                "message", notification.getMessage(),
                "isRead", notification.getIsRead(),
                "createdAt", notification.getCreatedAt(),
                "relatedId", notification.getRelatedId(),
                "relatedType", notification.getRelatedType()
            );
            webSocketHandler.sendNotification(notification.getRecipientId(), payload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }
    }

    public void handleNotificationEvent(NotificationEvent event) {
        // Handle async notification events
        log.info("Handling notification event: {}", event);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        String actorName = "System";
        try {
            if (notification.getActorId() != null) {
                // Try to get user name from auth service
                // actorName = authClient.getUserById(notification.getActorId()).getName();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch actor name for user {}", notification.getActorId());
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .actorId(notification.getActorId())
                .actorName(actorName)
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .deepLink(notification.getDeepLink())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
