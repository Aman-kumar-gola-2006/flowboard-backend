package com.flowboard.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author Aman Kumar Gola
 * Starting point for Board Service.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BoardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoardServiceApplication.class, args);
        System.out.println("Board Service started on port 8083...");
    }
}
