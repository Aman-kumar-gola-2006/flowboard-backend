package com.flowboard.auth.service;

import com.flowboard.auth.dto.NotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.notification.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.notification.routing-key}")
    private String routingKey;

    public void sendNotification(NotificationMessage message) {
        System.out.println("Sending message to RabbitMQ: " + message);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
