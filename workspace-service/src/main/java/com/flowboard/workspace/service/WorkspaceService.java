package com.flowboard.workspace.service;

import com.flowboard.workspace.client.AuthServiceClient;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.model.Workspace;
import com.flowboard.workspace.model.WorkspaceInvitation;
import com.flowboard.workspace.model.WorkspaceMember;
import com.flowboard.workspace.repository.WorkspaceInvitationRepository;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {
    
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final AuthServiceClient authServiceClient;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RestTemplate restTemplate;
    
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
        member.setStatus("ACTIVE");
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

    public List<WorkspaceResponse> getPendingInvitations(Long userId, String authToken) {
        List<Workspace> workspaces = workspaceRepository.findPendingByMemberUserId(userId);
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
        if (request.getIsPro() != null) {
            workspace.setIsPro(request.getIsPro());
        }
        
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        return mapToWorkspaceResponse(updatedWorkspace, authToken);
    }
    
    @Transactional
    public WorkspaceResponse upgradeWorkspace(Long id) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found with id: " + id));
        workspace.setIsPro(true);
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        return mapToWorkspaceResponse(updatedWorkspace, null);
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
    public MemberResponse addMember(Long workspaceId, MemberRequest request, Long actorId, String authToken) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
        
        UserResponse user = null;
        try {
            user = authServiceClient.getUserByEmail(request.getEmail(), authToken);
        } catch (Exception e) {
            log.info("User {} not found in Auth Service. Creating external invitation.", request.getEmail());
        }
        
        if (user != null) {
            // Existing User Flow
            if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
                throw new RuntimeException("User is already a member of this workspace");
            }
            
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(workspace);
            member.setUserId(user.getId());
            member.setRole(request.getRole() != null ? request.getRole() : "MEMBER");
            member.setStatus("PENDING");
            
            WorkspaceMember savedMember = memberRepository.save(member);
            
            // Send email
            sendInvitationEmail(request.getEmail(), workspace.getName(), false, null);

            // Send in-app notification
            sendInAppNotification(user.getId(), actorId != null ? actorId : workspace.getOwnerId(), workspaceId, workspace.getName());
            
            return mapToMemberResponse(savedMember, user);
        } else {
            // New User (External Invitation) Flow
            Optional<WorkspaceInvitation> existingInvite = invitationRepository.findByEmailAndWorkspaceId(request.getEmail(), workspaceId);
            if (existingInvite.isPresent()) {
                throw new RuntimeException("An invitation has already been sent to this email for this workspace.");
            }

            WorkspaceInvitation invitation = new WorkspaceInvitation();
            invitation.setWorkspaceId(workspaceId);
            invitation.setEmail(request.getEmail());
            invitation.setRole(request.getRole() != null ? request.getRole() : "MEMBER");
            invitation.setInvitedBy(actorId != null ? actorId : workspace.getOwnerId());
            
            WorkspaceInvitation savedInvite = invitationRepository.save(invitation);
            
            // Send email with token
            sendInvitationEmail(request.getEmail(), workspace.getName(), true, savedInvite.getToken());

            return MemberResponse.builder()
                    .userEmail(request.getEmail())
                    .userName("Invited User")
                    .role(invitation.getRole())
                    .status("INVITED")
                    .build();
        }
    }

    private void sendInvitationEmail(String email, String workspaceName, boolean isNewUser, String token) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("FlowBoard - Invitation to join " + workspaceName);
            
            String link = isNewUser 
                ? "http://localhost:4200/register?inviteToken=" + token 
                : "http://localhost:4200/login";
            
            String message = "You've been invited to join the workspace \"" + workspaceName + "\" on FlowBoard.\n\n";
            if (isNewUser) {
                message += "Since you don't have an account yet, please register using the link below to accept the invitation:\n";
            } else {
                message += "Please login to your account to view the invitation:\n";
            }
            message += link;
            
            msg.setText(message);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", email, e.getMessage());
        }
    }

    private void sendInAppNotification(Long recipientId, Long actorId, Long workspaceId, String workspaceName) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("recipientId", recipientId);
            notification.put("actorId", actorId);
            notification.put("type", "WORKSPACE_INVITE");
            notification.put("title", "Workspace Invitation");
            notification.put("message", "You have been invited to join workspace: " + workspaceName);
            notification.put("relatedId", workspaceId);
            notification.put("relatedType", "WORKSPACE");
            
            restTemplate.postForObject("http://notification-service/api/notifications/send", 
                notification, Object.class);
        } catch (Exception e) {
            log.error("In-app notification failed for user {}: {}", recipientId, e.getMessage());
        }
    }

    @Transactional
    public void acceptInvitationByToken(String token, Long userId) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invitation token"));
        
        if (invitation.getIsAccepted() || invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This invitation has expired or already been accepted");
        }

        Workspace workspace = workspaceRepository.findById(invitation.getWorkspaceId())
                .orElseThrow(() -> new RuntimeException("Workspace no longer exists"));

        // Create member
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUserId(userId);
        member.setRole(invitation.getRole());
        member.setStatus("ACTIVE");
        memberRepository.save(member);

        // Mark invitation as accepted
        invitation.setIsAccepted(true);
        invitationRepository.save(invitation);
        
        log.info("User {} accepted invitation for workspace {} via token", userId, workspace.getId());
    }

    public WorkspaceInvitation validateInvitation(String token) {
        return invitationRepository.findByToken(token)
                .filter(i -> !i.getIsAccepted() && i.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new RuntimeException("Invalid or expired invitation token"));
    }
    
    @Transactional
    public void removeMember(Long workspaceId, Long userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found in this workspace"));
        
        memberRepository.delete(member);
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
                    try {
                        UserResponse user = authServiceClient.getUserById(member.getUserId(), authToken);
                        return mapToMemberResponse(member, user);
                    } catch (Exception e) {
                        log.warn("User {} not found in Auth Service for workspace member entry", member.getUserId());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
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
        if (authToken != null && !authToken.isEmpty()) {
            try {
                owner = authServiceClient.getUserById(workspace.getOwnerId(), authToken);
            } catch (Exception e) {
                log.error("Error fetching owner details: {}", e.getMessage());
            }
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
                .isPro(workspace.getIsPro())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .memberCount(members.size())
                .boardCount(0)
                .build();
    }
    
    @Transactional
    public void acceptInvitation(Long workspaceId, Long userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        
        if ("ACTIVE".equals(member.getStatus())) {
            return; // Already accepted
        }
        
        member.setStatus("ACTIVE");
        memberRepository.save(member);

        // Send notification to workspace owner
        try {
            Workspace workspace = member.getWorkspace();
            Map<String, Object> notification = new HashMap<>();
            notification.put("recipientId", workspace.getOwnerId());
            notification.put("actorId", userId);
            notification.put("type", "INVITE_ACCEPTED");
            notification.put("title", "Invitation Accepted");
            notification.put("message", "User has joined your workspace: " + workspace.getName());
            notification.put("relatedId", workspaceId);
            notification.put("relatedType", "WORKSPACE");
            
            restTemplate.postForObject("http://notification-service/api/notifications/send", 
                notification, Object.class);
        } catch (Exception e) {
            log.error("Failed to send acceptance notification: {}", e.getMessage());
        }
    }
    
    @Transactional
    public void rejectInvitation(Long workspaceId, Long userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        memberRepository.delete(member);
    }
    
    private MemberResponse mapToMemberResponse(WorkspaceMember member, UserResponse user) {
        return MemberResponse.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .userAvatar(user.getAvatarUrl())
                .role(member.getRole())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
