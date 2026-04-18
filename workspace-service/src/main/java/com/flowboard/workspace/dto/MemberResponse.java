package com.flowboard.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userAvatar;
    private String role;
    private LocalDateTime joinedAt;
}
