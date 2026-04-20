package com.flowboard.notification.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long recipientId;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private String deepLink;
}
