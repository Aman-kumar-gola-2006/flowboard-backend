package com.flowboard.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage implements Serializable {
    private String email;
    private String name;
    private String type; // WELCOME, OTP, PRO, SUSPEND, REACTIVATE
    private String extraData; // For OTP or other specific info
}
