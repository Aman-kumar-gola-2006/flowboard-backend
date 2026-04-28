package com.flowboard.workspace.controller;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkspaceController {
    
    private final WorkspaceService workspaceService;
    
    @PostMapping
    public ResponseEntity<?> createWorkspace(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authToken,
            @Valid @RequestBody WorkspaceRequest request) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("X-User-Id header is missing. Please re-login.", false));
            }
            if (authToken == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Authorization header is missing. Please re-login.", false));
            }
            
            WorkspaceResponse response = workspaceService.createWorkspace(userId, request, authToken);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authToken) {
        WorkspaceResponse response = workspaceService.getWorkspaceById(id, authToken);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<WorkspaceResponse>> getWorkspacesByOwner(
            @PathVariable Long ownerId,
            @RequestHeader("Authorization") String authToken) {
        List<WorkspaceResponse> responses = workspaceService.getWorkspacesByOwner(ownerId, authToken);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/member/{userId}")
    public ResponseEntity<List<WorkspaceResponse>> getWorkspacesByMember(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authToken) {
        List<WorkspaceResponse> responses = workspaceService.getWorkspacesByMember(userId, authToken);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/member/{userId}/pending")
    public ResponseEntity<List<WorkspaceResponse>> getPendingInvitations(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authToken) {
        List<WorkspaceResponse> responses = workspaceService.getPendingInvitations(userId, authToken);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspacesForUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authToken) {
        List<WorkspaceResponse> responses = workspaceService.getAllWorkspacesForUser(userId, authToken);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/public")
    public ResponseEntity<List<WorkspaceResponse>> getPublicWorkspaces(
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        List<WorkspaceResponse> responses = workspaceService.getPublicWorkspaces(authToken);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authToken,
            @Valid @RequestBody WorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.updateWorkspace(id, request, authToken);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.ok(new MessageResponse("Workspace deleted successfully", true));
    }
    
    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader("Authorization") String authToken,
            @Valid @RequestBody MemberRequest request) {
        MemberResponse response = workspaceService.addMember(workspaceId, request, actorId, authToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<MessageResponse> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        workspaceService.removeMember(workspaceId, userId);
        return ResponseEntity.ok(new MessageResponse("Member removed successfully", true));
    }
    
    @PutMapping("/{workspaceId}/members/{userId}/role")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authToken,
            @Valid @RequestBody UpdateRoleRequest request) {
        MemberResponse response = workspaceService.updateMemberRole(workspaceId, userId, request, authToken);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<MemberResponse>> getWorkspaceMembers(
            @PathVariable Long workspaceId,
            @RequestHeader("Authorization") String authToken) {
        List<MemberResponse> responses = workspaceService.getWorkspaceMembers(workspaceId, authToken);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{workspaceId}/members/{userId}/exists")
    public ResponseEntity<Boolean> checkMembership(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        boolean exists = workspaceService.isUserMemberOfWorkspace(workspaceId, userId);
        return ResponseEntity.ok(exists);
    }
    
    @PostMapping("/{workspaceId}/members/{userId}/accept")
    public ResponseEntity<MessageResponse> acceptInvitation(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        workspaceService.acceptInvitation(workspaceId, userId);
        return ResponseEntity.ok(new MessageResponse("Invitation accepted", true));
    }
    
    @PostMapping("/{workspaceId}/members/{userId}/reject")
    public ResponseEntity<MessageResponse> rejectInvitation(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        workspaceService.rejectInvitation(workspaceId, userId);
        return ResponseEntity.ok(new MessageResponse("Invitation rejected", true));
    }
    
    @GetMapping("/{workspaceId}/members/{userId}/is-admin")
    public ResponseEntity<Boolean> checkAdmin(
            @PathVariable Long workspaceId,
            @PathVariable Long userId) {
        boolean isAdmin = workspaceService.isUserAdminOfWorkspace(workspaceId, userId);
        return ResponseEntity.ok(isAdmin);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces(@RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(workspaceService.getAllWorkspacesForAdmin(authToken));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalWorkspaces() {
        return ResponseEntity.ok(workspaceService.getTotalCount());
    }
}
