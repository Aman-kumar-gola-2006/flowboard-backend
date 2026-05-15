package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.NotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.notification.queue}")
    private String queueName;

    public void sendNotification(NotificationMessage message) {
        log.info("Sending notification message to RabbitMQ: {}", message.getType());
        try {
            rabbitTemplate.convertAndSend(queueName, message);
            log.info("Message successfully sent to queue: {}", queueName);
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage());
        }
    }
}
