package com.flowboard.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long cardId;
    private Long authorId;
    private String authorName;  // we'll fetch from Auth later
    private String content;
    private Long parentCommentId;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies; // nested replies
}
