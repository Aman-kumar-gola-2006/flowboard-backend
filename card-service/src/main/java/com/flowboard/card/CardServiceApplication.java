package com.flowboard.card;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardServiceApplication.class, args);
        System.out.println("Card Service started on port 8085...");
    }
}
