package com.flowboard.workspace.service;

import com.flowboard.workspace.client.AuthServiceClient;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.model.Workspace;
import com.flowboard.workspace.model.WorkspaceMember;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {
    
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final AuthServiceClient authServiceClient;
    
    @Transactional
    public WorkspaceResponse createWorkspace(Long ownerId, WorkspaceRequest request, String authToken) {
        log.info("Creating workspace '{}' for user: {}", request.getName(), ownerId);
        
        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());
        workspace.setOwnerId(ownerId);
        workspace.setVisibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE");
        workspace.setLogoUrl(request.getLogoUrl());
        
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(savedWorkspace);
        member.setUserId(ownerId);
        member.setRole("ADMIN");
        memberRepository.save(member);
        
        log.info("Workspace created successfully with ID: {}", savedWorkspace.getId());
        
        return mapToWorkspaceResponse(savedWorkspace, authToken);
    }
    
    public WorkspaceResponse getWorkspaceById(Long id, String authToken) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found with id: " + id));
        return mapToWorkspaceResponse(workspace, authToken);
    }
    
    public List<WorkspaceResponse> getWorkspacesByOwner(Long ownerId, String authToken) {
        List<Workspace> workspaces = workspaceRepository.findByOwnerIdAndIsActive(ownerId, true);
        return workspaces.stream()
                .map(w -> mapToWorkspaceResponse(w, authToken))
                .collect(Collectors.toList());
    }
    
    public List<WorkspaceResponse> getWorkspacesByMember(Long userId, String authToken) {
        List<Workspace> workspaces = workspaceRepository.findByMemberUserId(userId);
        return workspaces.stream()
                .filter(Workspace::getIsActive)
                .map(w -> mapToWorkspaceResponse(w, authToken))
                .collect(Collectors.toList());
    }
    
    public List<WorkspaceResponse> getAllWorkspacesForUser(Long userId, String authToken) {
        List<WorkspaceResponse> ownedWorkspaces = getWorkspacesByOwner(userId, authToken);
        List<WorkspaceResponse> memberWorkspaces = getWorkspacesByMember(userId, authToken);
        
        List<WorkspaceResponse> allWorkspaces = new ArrayList<>();
        allWorkspaces.addAll(ownedWorkspaces);
        allWorkspaces.addAll(memberWorkspaces);
        
        return allWorkspaces.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<WorkspaceResponse> getPublicWorkspaces(String authToken) {
        List<Workspace> workspaces = workspaceRepository.findByVisibility("PUBLIC");
        return workspaces.stream()
                .filter(Workspace::getIsActive)
                .map(w -> mapToWorkspaceResponse(w, authToken))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public WorkspaceResponse updateWorkspace(Long id, WorkspaceRequest request, String authToken) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found with id: " + id));
        
        if (request.getName() != null) {
            workspace.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }
        if (request.getVisibility() != null) {
            workspace.setVisibility(request.getVisibility());
        }
        if (request.getLogoUrl() != null) {
            workspace.setLogoUrl(request.getLogoUrl());
        }
        
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        return mapToWorkspaceResponse(updatedWorkspace, authToken);
    }
    
    @Transactional
    public void deleteWorkspace(Long id) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found with id: " + id));
        workspace.setIsActive(false);
        workspaceRepository.save(workspace);
    }
    
    @Transactional
    public void hardDeleteWorkspace(Long id) {
        workspaceRepository.deleteById(id);
    }
    
    @Transactional
    public MemberResponse addMember(Long workspaceId, MemberRequest request, String authToken) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        
        UserResponse user = authServiceClient.getUserByEmail(request.getEmail(), authToken);
        
        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new RuntimeException("User is already a member of this workspace");
        }
        
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUserId(user.getId());
        member.setRole(request.getRole() != null ? request.getRole() : "MEMBER");
        
        WorkspaceMember savedMember = memberRepository.save(member);
        
        return mapToMemberResponse(savedMember, user);
    }
    
    @Transactional
    public void removeMember(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new RuntimeException("User is not a member of this workspace");
        }
        memberRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
    }
    
    @Transactional
    public MemberResponse updateMemberRole(Long workspaceId, Long userId, UpdateRoleRequest request, String authToken) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        member.setRole(request.getRole());
        WorkspaceMember updatedMember = memberRepository.save(member);
        
        UserResponse user = authServiceClient.getUserById(userId, authToken);
        
        return mapToMemberResponse(updatedMember, user);
    }
    
    public List<MemberResponse> getWorkspaceMembers(Long workspaceId, String authToken) {
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        return members.stream()
                .map(member -> {
                    UserResponse user = authServiceClient.getUserById(member.getUserId(), authToken);
                    return mapToMemberResponse(member, user);
                })
                .collect(Collectors.toList());
    }
    
    public boolean isUserMemberOfWorkspace(Long workspaceId, Long userId) {
        return memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }
    
    public boolean isUserAdminOfWorkspace(Long workspaceId, Long userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(member -> "ADMIN".equals(member.getRole()))
                .orElse(false);
    }

    public long getTotalCount() {
        return workspaceRepository.count();
    }

    public List<WorkspaceResponse> getAllWorkspacesForAdmin(String authToken) {
        return workspaceRepository.findAll().stream()
                .map(w -> mapToWorkspaceResponse(w, authToken))
                .collect(Collectors.toList());
    }
    
    private WorkspaceResponse mapToWorkspaceResponse(Workspace workspace, String authToken) {
        UserResponse owner = null;
        try {
            owner = authServiceClient.getUserById(workspace.getOwnerId(), authToken);
        } catch (Exception e) {
            log.error("Error fetching owner details: {}", e.getMessage());
        }
        
        List<WorkspaceMember> members = memberRepository.findByWorkspace(workspace);
        
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwnerId())
                .ownerName(owner != null ? owner.getFullName() : "Unknown")
                .visibility(workspace.getVisibility())
                .logoUrl(workspace.getLogoUrl())
                .isActive(workspace.getIsActive())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .memberCount(members.size())
                .boardCount(0)
                .build();
    }
    
    private MemberResponse mapToMemberResponse(WorkspaceMember member, UserResponse user) {
        return MemberResponse.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .userAvatar(user.getAvatarUrl())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
