package com.flowboard.workspace.dto;

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
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private String visibility;
    private String logoUrl;
    private Boolean isActive;
    private Boolean isPro;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MemberResponse> members;
    private Integer memberCount;
    private Integer boardCount;
}
