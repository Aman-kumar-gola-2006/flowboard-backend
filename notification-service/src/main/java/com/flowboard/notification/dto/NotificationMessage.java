package com.flowboard.notification.dto;

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
    private String type; // WELCOME, OTP, PRO, SUSPEND, REACTIVATE, INVITE, ASSIGN, CONTACT
    private String extraData; // For OTP, token, or problem description
    private String workspaceName;
    private String boardName;
    private String taskTitle;
    private String inviterName;
    private Long recipientId;
    private Long actorId;
    private Long relatedId;
    private String relatedType;
}
