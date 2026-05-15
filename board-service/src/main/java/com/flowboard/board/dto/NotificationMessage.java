package com.flowboard.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage implements Serializable {
    private String email;
    private String name;
    private String type;
    private String extraData;
    private String workspaceName;
    private String boardName;
    private String taskTitle;
    private String inviterName;
    private Long recipientId;
    private Long actorId;
}
