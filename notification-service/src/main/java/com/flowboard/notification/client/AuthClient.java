package com.flowboard.notification.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "auth-client")
public interface AuthClient {

}
