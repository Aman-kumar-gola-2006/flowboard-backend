package com.flowboard.notification.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "card-client")
public interface CardClient {

}
