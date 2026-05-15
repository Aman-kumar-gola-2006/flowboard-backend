package com.flowboard.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.notification.queue}")
    private String notificationQueue;

    @Bean
    public Queue notificationQueue() {
        return new Queue(notificationQueue, true);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        org.springframework.amqp.support.converter.Jackson2JsonMessageConverter converter = new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();

        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper() {
            @Override
            public Class<?> toClass(org.springframework.amqp.core.MessageProperties properties) {
                return com.flowboard.notification.dto.NotificationMessage.class;
            }
        };
        classMapper.setTrustedPackages("*");

        converter.setClassMapper(classMapper);
        return converter;
    }
}
