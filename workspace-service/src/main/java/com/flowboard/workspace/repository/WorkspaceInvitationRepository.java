package com.flowboard.workspace.repository;

import com.flowboard.workspace.model.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    Optional<WorkspaceInvitation> findByToken(String token);
    Optional<WorkspaceInvitation> findByEmailAndWorkspaceId(String email, Long workspaceId);
    List<WorkspaceInvitation> findByEmail(String email);
    void deleteByToken(String token);
}
