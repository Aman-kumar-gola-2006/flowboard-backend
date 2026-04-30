package com.flowboard.workspace.service;

import com.flowboard.workspace.client.AuthServiceClient;
import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.model.Workspace;
import com.flowboard.workspace.model.WorkspaceMember;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceService Unit Tests")
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private JavaMailSender mailSender;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private WorkspaceService workspaceService;

    private Workspace testWorkspace;
    private WorkspaceMember adminMember;
    private UserResponse testUser;

    @BeforeEach
    void setUp() {
        testWorkspace = new Workspace();
        testWorkspace.setId(1L);
        testWorkspace.setName("Test Workspace");
        testWorkspace.setDescription("A test workspace");
        testWorkspace.setOwnerId(1L);
        testWorkspace.setVisibility("PRIVATE");
        testWorkspace.setIsActive(true);
        testWorkspace.setIsPro(false);

        adminMember = new WorkspaceMember();
        adminMember.setId(1L);
        adminMember.setWorkspace(testWorkspace);
        adminMember.setUserId(1L);
        adminMember.setRole("ADMIN");
        adminMember.setStatus("ACTIVE");

        testUser = UserResponse.builder()
                .id(1L)
                .email("owner@flowboard.com")
                .username("owner")
                .fullName("Owner User")
                .build();
    }

    // ========== CREATE WORKSPACE ==========

    @Test
    @DisplayName("CreateWorkspace - creates workspace and adds owner as admin member")
    void createWorkspace_ShouldCreateAndAddOwnerAsAdmin() {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName("New Workspace");
        request.setDescription("Description");
        request.setVisibility("PRIVATE");

        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);
        when(memberRepository.save(any(WorkspaceMember.class))).thenReturn(adminMember);
        when(authServiceClient.getUserById(eq(1L), anyString())).thenReturn(testUser);
        when(memberRepository.findByWorkspace(any(Workspace.class))).thenReturn(List.of(adminMember));

        WorkspaceResponse result = workspaceService.createWorkspace(1L, request, "Bearer token");

        assertNotNull(result);
        assertEquals("Test Workspace", result.getName());
        verify(workspaceRepository).save(any(Workspace.class));
        verify(memberRepository).save(any(WorkspaceMember.class));
    }

    // ========== GET WORKSPACE BY ID ==========

    @Test
    @DisplayName("GetWorkspaceById - returns workspace when found")
    void getWorkspaceById_WithValidId_ShouldReturnWorkspace() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(authServiceClient.getUserById(eq(1L), anyString())).thenReturn(testUser);
        when(memberRepository.findByWorkspace(testWorkspace)).thenReturn(List.of(adminMember));

        WorkspaceResponse result = workspaceService.getWorkspaceById(1L, "Bearer token");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Workspace", result.getName());
    }

    @Test
    @DisplayName("GetWorkspaceById - throws when workspace not found")
    void getWorkspaceById_WithInvalidId_ShouldThrowException() {
        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workspaceService.getWorkspaceById(999L, "token"));
    }

    // ========== GET WORKSPACES BY OWNER ==========

    @Test
    @DisplayName("GetWorkspacesByOwner - returns owner's active workspaces")
    void getWorkspacesByOwner_ShouldReturnWorkspaces() {
        when(workspaceRepository.findByOwnerIdAndIsActive(1L, true)).thenReturn(List.of(testWorkspace));
        when(authServiceClient.getUserById(eq(1L), anyString())).thenReturn(testUser);
        when(memberRepository.findByWorkspace(testWorkspace)).thenReturn(List.of(adminMember));

        List<WorkspaceResponse> results = workspaceService.getWorkspacesByOwner(1L, "token");

        assertThat(results).hasSize(1);
        assertEquals("Test Workspace", results.get(0).getName());
    }

    // ========== UPDATE WORKSPACE ==========

    @Test
    @DisplayName("UpdateWorkspace - success when workspace exists")
    void updateWorkspace_WhenExists_ShouldUpdate() {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);
        when(authServiceClient.getUserById(any(), anyString())).thenReturn(testUser);
        when(memberRepository.findByWorkspace(any())).thenReturn(List.of(adminMember));

        WorkspaceResponse result = workspaceService.updateWorkspace(1L, request, "token");

        assertNotNull(result);
        assertEquals("Updated Name", testWorkspace.getName());
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    @DisplayName("UpdateWorkspace - throws when workspace not found")
    void updateWorkspace_WhenNotFound_ShouldThrowException() {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName("Updated");

        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workspaceService.updateWorkspace(999L, request, "token"));
    }

    // ========== DELETE WORKSPACE (soft) ==========

    @Test
    @DisplayName("DeleteWorkspace - sets isActive to false (soft delete)")
    void deleteWorkspace_ShouldSetInactive() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        assertDoesNotThrow(() -> workspaceService.deleteWorkspace(1L));
        assertFalse(testWorkspace.getIsActive());
    }

    // ========== IS MEMBER / IS ADMIN ==========

    @Test
    @DisplayName("IsUserMemberOfWorkspace - returns true when member exists")
    void isUserMemberOfWorkspace_WhenMember_ShouldReturnTrue() {
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L)).thenReturn(true);

        assertTrue(workspaceService.isUserMemberOfWorkspace(1L, 1L));
    }

    @Test
    @DisplayName("IsUserMemberOfWorkspace - returns false when not member")
    void isUserMemberOfWorkspace_WhenNotMember_ShouldReturnFalse() {
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 99L)).thenReturn(false);

        assertFalse(workspaceService.isUserMemberOfWorkspace(1L, 99L));
    }

    @Test
    @DisplayName("IsUserAdminOfWorkspace - returns true when admin")
    void isUserAdminOfWorkspace_WhenAdmin_ShouldReturnTrue() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertTrue(workspaceService.isUserAdminOfWorkspace(1L, 1L));
    }

    @Test
    @DisplayName("IsUserAdminOfWorkspace - returns false when regular member")
    void isUserAdminOfWorkspace_WhenMember_ShouldReturnFalse() {
        WorkspaceMember regularMember = new WorkspaceMember();
        regularMember.setUserId(2L);
        regularMember.setRole("MEMBER");

        when(memberRepository.findByWorkspaceIdAndUserId(1L, 2L)).thenReturn(Optional.of(regularMember));

        assertFalse(workspaceService.isUserAdminOfWorkspace(1L, 2L));
    }

    @Test
    @DisplayName("IsUserAdminOfWorkspace - returns false when user not in workspace")
    void isUserAdminOfWorkspace_WhenNotInWorkspace_ShouldReturnFalse() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertFalse(workspaceService.isUserAdminOfWorkspace(1L, 99L));
    }

    // ========== REMOVE MEMBER ==========

    @Test
    @DisplayName("RemoveMember - deletes member successfully")
    void removeMember_WhenMemberExists_ShouldDelete() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        doNothing().when(memberRepository).delete(adminMember);

        assertDoesNotThrow(() -> workspaceService.removeMember(1L, 1L));
        verify(memberRepository).delete(adminMember);
    }

    @Test
    @DisplayName("RemoveMember - throws when member not found")
    void removeMember_WhenNotFound_ShouldThrowException() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workspaceService.removeMember(1L, 99L));
    }

    // ========== UPGRADE WORKSPACE ==========

    @Test
    @DisplayName("UpgradeWorkspace - sets isPro to true")
    void upgradeWorkspace_ShouldSetPro() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);
        when(memberRepository.findByWorkspace(any())).thenReturn(List.of(adminMember));

        WorkspaceResponse result = workspaceService.upgradeWorkspace(1L);

        assertNotNull(result);
        assertTrue(testWorkspace.getIsPro());
    }

    // ========== GET TOTAL COUNT ==========

    @Test
    @DisplayName("GetTotalCount - returns workspace count")
    void getTotalCount_ShouldReturnCount() {
        when(workspaceRepository.count()).thenReturn(3L);
        assertEquals(3L, workspaceService.getTotalCount());
    }

    // ========== ACCEPT / REJECT INVITATION ==========

    @Test
    @DisplayName("AcceptInvitation - sets member status to ACTIVE")
    void acceptInvitation_WhenPending_ShouldSetActive() {
        adminMember.setStatus("PENDING");
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.save(any(WorkspaceMember.class))).thenReturn(adminMember);

        assertDoesNotThrow(() -> workspaceService.acceptInvitation(1L, 1L));
        assertEquals("ACTIVE", adminMember.getStatus());
    }

    @Test
    @DisplayName("AcceptInvitation - no-op when already ACTIVE")
    void acceptInvitation_WhenAlreadyActive_ShouldDoNothing() {
        adminMember.setStatus("ACTIVE");
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertDoesNotThrow(() -> workspaceService.acceptInvitation(1L, 1L));
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("RejectInvitation - deletes member")
    void rejectInvitation_ShouldDeleteMember() {
        adminMember.setStatus("PENDING");
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        doNothing().when(memberRepository).delete(adminMember);

        assertDoesNotThrow(() -> workspaceService.rejectInvitation(1L, 1L));
        verify(memberRepository).delete(adminMember);
    }
}
