package com.flowboard.notification.service;

import com.flowboard.notification.client.AuthClient;
import com.flowboard.notification.dto.NotificationRequest;
import com.flowboard.notification.dto.NotificationResponse;
import com.flowboard.notification.event.NotificationEvent;
import com.flowboard.notification.model.Notification;
import com.flowboard.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthClient authClient;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    @Transactional
    public Notification createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setRecipientId(request.getRecipientId());
        notification.setActorId(request.getActorId());
        notification.setActorName(request.getActorName());
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
        
        // Send email if user has email notifications enabled
        if ("CHAT_MESSAGE".equals(saved.getType())) {
            try {
                // Fetch the user to get email and settings
                Map<String, Object> userProfile = authClient.getUserById(saved.getRecipientId());
                if (userProfile != null && userProfile.containsKey("email")) {
                    String email = (String) userProfile.get("email");
                    String username = (String) userProfile.get("username");
                    
                    boolean emailEnabled = true;
                    if (userProfile.containsKey("emailNotifications")) {
                        emailEnabled = Boolean.TRUE.equals(userProfile.get("emailNotifications"));
                    }
                    
                    if (emailEnabled) {
                        String emailBody = "Hi " + (username != null ? username : "User") + ",<br><br>" +
                                           "You have received a new message in board chat:<br><br>" +
                                           "<div style=\"background-color: #f1f5f9; border-radius: 16px; padding: 24px; margin: 24px 0;\">" +
                                           "  <strong>" + saved.getMessage() + "</strong>" +
                                           "</div>" +
                                           "Log in to FlowBoard to open the board and join the conversation.";
                        
                        emailService.sendEmail(email, "New Message in Board Chat: " + saved.getTitle(), emailBody, true);
                        saved.setIsEmailSent(true);
                        notificationRepository.save(saved);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send chat message email to recipient {}: {}", saved.getRecipientId(), e.getMessage());
            }
        }
        
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
            String taskTitle = (String) card.get("title");
            
            emailService.sendEmail(email, 
                "FlowBoard - Task Due Soon!",
                "Your card '" + taskTitle + "' is due in 1 hour!", 
                false);
        } catch (Exception e) {
            log.error("Email failed: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        try {
            return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Database error fetching notifications: {}", e.getMessage());
            throw e;
        }
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

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteByRecipientId(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotificationsForAdmin() {
        return notificationRepository.findAll();
    }

    @Transactional
    public void sendWebSocketNotification(Notification notification) {
        try {
            NotificationResponse response = mapToResponse(notification);
            log.info("Sending STOMP notification to user {}: /topic/notifications/{}", 
                     notification.getRecipientId(), notification.getRecipientId());
            
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getRecipientId(),
                response
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification via STOMP", e);
        }
    }

    public void handleNotificationEvent(NotificationEvent event) {
        // Handle async notification events
        log.info("Handling notification event: {}", event);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        String actorName = notification.getActorName() != null ? notification.getActorName() : "System";

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
