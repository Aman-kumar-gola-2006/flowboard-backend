package com.flowboard.label;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class LabelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelServiceApplication.class, args);
        System.out.println("Label Service started on port 8087...");
    }
}
