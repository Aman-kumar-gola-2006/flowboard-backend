package com.flowboard.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long id;
    private Long recipientId;
    private Long actorId;
    private String type; // ASSIGNMENT, MENTION, DUE_DATE, COMMENT, MOVE
    private String title;
    private String message;
    private Long relatedId; // cardId, boardId, etc.
    private String relatedType; // CARD, BOARD, COMMENT
    private String deepLink;
    private LocalDateTime timestamp;
    private String sourceService; // Which service triggered this
}
