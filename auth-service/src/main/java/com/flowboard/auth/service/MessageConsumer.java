package com.flowboard.auth.service;

import com.flowboard.auth.dto.NotificationMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = "${spring.rabbitmq.notification.queue}")
    public void consume(NotificationMessage message) {
        System.out.println("Message received from RabbitMQ: " + message);
        
        try {
            switch (message.getType()) {
                case "WELCOME":
                    emailService.sendWelcomeEmail(message.getEmail(), message.getName());
                    break;
                case "OTP":
                    emailService.sendOtpEmail(message.getEmail(), message.getExtraData());
                    break;
                case "PRO":
                    emailService.sendProUpgradeEmail(message.getEmail(), message.getName());
                    break;
                case "SUSPEND":
                    emailService.sendSuspensionEmail(message.getEmail(), message.getName());
                    break;
                case "REACTIVATE":
                    emailService.sendReactivationEmail(message.getEmail(), message.getName());
                    break;
                default:
                    System.out.println("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            System.err.println("Error processing message from RabbitMQ: " + e.getMessage());
        }
    }
}
