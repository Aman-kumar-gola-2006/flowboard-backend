package com.flowboard.comment.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Long cardId;
    private String content;
    private Long parentCommentId; // optional, for replies
}
