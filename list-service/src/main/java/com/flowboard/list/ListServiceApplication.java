package com.flowboard.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author Aman Kumar Gola
 * List Service - Manages Kanban columns/lists
 * Nothing fancy, just standard Spring Boot setup
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ListServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListServiceApplication.class, args);
        System.out.println("List Service started on port 8084...");
    }
}
