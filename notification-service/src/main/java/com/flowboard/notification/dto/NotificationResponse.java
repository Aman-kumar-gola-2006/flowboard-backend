package com.flowboard.notification.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Long recipientId;
    private Long actorId;
    private String actorName;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
    private String deepLink;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
