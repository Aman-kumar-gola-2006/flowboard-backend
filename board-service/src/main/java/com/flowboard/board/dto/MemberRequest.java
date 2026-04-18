package com.flowboard.board.dto;

import lombok.Data;

@Data
public class MemberRequest {
    private Long userId;
    private String role; // ADMIN, MEMBER, OBSERVER
}
