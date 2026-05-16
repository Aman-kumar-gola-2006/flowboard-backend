package com.flowboard.notification.service;

import com.flowboard.notification.dto.NotificationMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageConsumer {

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;
 
    @RabbitListener(queues = "${spring.rabbitmq.notification.queue}")
    public void consume(NotificationMessage message) {
        log.info("Notification message received from RabbitMQ: type={}, email={}", message.getType(), message.getEmail());
        
        try {
            switch (message.getType()) {
                case "WELCOME":
                    emailService.sendWelcomeEmail(message.getEmail(), message.getName());
                    saveNotification(message, "Welcome", "Welcome to FlowBoard!");
                    break;
                case "OTP":
                    emailService.sendOtpEmail(message.getEmail(), message.getExtraData());
                    break;
                case "PRO":
                    emailService.sendProUpgradeEmail(message.getEmail(), message.getName());
                    saveNotification(message, "PRO Upgrade", "You are now a PRO member!");
                    break;
                case "INVITE":
                    emailService.sendInvitationEmail(message.getEmail(), message.getInviterName(), message.getWorkspaceName(), message.getExtraData());
                    saveNotification(message, "Workspace Invitation", message.getInviterName() + " invited you to " + message.getWorkspaceName());
                    break;
                case "ASSIGN":
                    emailService.sendTaskAssignmentEmail(message.getEmail(), message.getName(), message.getTaskTitle(), message.getBoardName(), message.getWorkspaceName());
                    saveNotification(message, "Task Assigned", message.getInviterName() + " assigned you: " + message.getTaskTitle());
                    break;
                case "SUSPEND":
                    emailService.sendSuspensionEmail(message.getEmail(), message.getName());
                    saveNotification(message, "Account Blocked", "Your account has been blocked by the administrator.");
                    break;
                case "REACTIVATE":
                    log.info("Processing REACTIVATE for user: {}", message.getEmail());
                    emailService.sendReactivationEmail(message.getEmail(), message.getName());
                    saveNotification(message, "Account Unblocked", "Your account has been unblocked. You can now log in again.");
                    break;
                case "CONTACT":
                    emailService.sendSupportEmail(message.getName(), message.getEmail(), "Support Request", message.getExtraData());
                    break;
                case "DUE_DATE":
                    saveNotification(message, "Due Date Reminder", "Task '" + message.getTaskTitle() + "' has a due date set: " + message.getExtraData());
                    break;
                default:
                    log.warn("Unknown message type received: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Error processing RabbitMQ notification message: {}", e.getMessage(), e);
        }
    }

    private void saveNotification(NotificationMessage message, String title, String text) {
        try {
            if (message.getRecipientId() != null) {
                com.flowboard.notification.dto.NotificationRequest request = new com.flowboard.notification.dto.NotificationRequest();
                request.setRecipientId(message.getRecipientId());
                request.setActorId(message.getActorId());
                request.setActorName(message.getInviterName());
                request.setType(message.getType());
                request.setTitle(title);
                request.setMessage(text);
                request.setRelatedId(message.getRelatedId());
                request.setRelatedType(message.getRelatedType());
                if (message.getExtraData() != null && (message.getExtraData().startsWith("http") || message.getExtraData().startsWith("/"))) {
                    request.setDeepLink(message.getExtraData());
                }
                notificationService.createNotification(request);
            }
        } catch (Exception e) {
            log.warn("Could not save notification to database: {}", e.getMessage());
        }
    }
}
