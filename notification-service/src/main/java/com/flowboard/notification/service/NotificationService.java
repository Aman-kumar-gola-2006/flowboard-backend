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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final AuthClient authClient;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

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

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = createNotification(request);
        return mapToResponse(notification);
    }

    @Scheduled(fixedRate = 3600000) // Every 1 hour
    public void checkDueDateReminders() {
        log.info("Checking due date reminders...");
        
        try {
            // Get all cards with due dates
            Object cardsResponse = restTemplate.getForObject(
                "http://localhost:8085/api/cards/overdue/all", Object.class);
            
            List<Map<String, Object>> cards = (List<Map<String, Object>>) cardsResponse;
            LocalDateTime now = LocalDateTime.now();
            
            for (Map<String, Object> card : cards) {
                String dueDateStr = (String) card.get("dueDate");
                if (dueDateStr == null) continue;
                
                LocalDateTime dueDate = LocalDateTime.parse(dueDateStr);
                long hoursUntilDue = java.time.Duration.between(now, dueDate).toHours();
                Long assigneeId = card.get("assigneeId") != null ? 
                    ((Number) card.get("assigneeId")).longValue() : null;
                
                // 1 day reminder
                if (hoursUntilDue <= 24 && hoursUntilDue > 1 && assigneeId != null) {
                    sendDueDateReminder(assigneeId, card);
                }
                
                // 1 hour reminder + email
                if (hoursUntilDue <= 1 && hoursUntilDue > 0 && assigneeId != null) {
                    sendDueDateReminder(assigneeId, card);
                    sendDueDateEmail(assigneeId, card);
                }
            }
        } catch (Exception e) {
            log.error("Due date check failed: {}", e.getMessage());
        }
    }

    private void sendDueDateReminder(Long userId, Map<String, Object> card) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(userId);
        request.setType("DUE_DATE");
        request.setTitle("Due Date Reminder");
        request.setMessage("Card '" + card.get("title") + "' is due soon!");
        request.setRelatedId(((Number) card.get("id")).longValue());
        request.setRelatedType("CARD");
        createNotification(request);
    }

    private void sendDueDateEmail(Long userId, Map<String, Object> card) {
        try {
            Object userResponse = restTemplate.getForObject(
                "http://localhost:8081/api/auth/users/" + userId, Object.class);
            Map<String, Object> user = (Map<String, Object>) userResponse;
            String email = (String) user.get("email");
            
            emailService.sendEmail(email, 
                "FlowBoard - Task Due Soon!",
                "Your card '" + card.get("title") + "' is due in 1 hour!");
        } catch (Exception e) {
            log.error("Email failed: {}", e.getMessage());
        }
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
