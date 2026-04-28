package com.flowboard.notification.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RestTemplateConfig {
    @org.springframework.context.annotation.Bean
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
