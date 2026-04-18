package com.flowboard.workspace.repository;

import com.flowboard.workspace.model.Workspace;
import com.flowboard.workspace.model.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    
    List<WorkspaceMember> findByWorkspace(Workspace workspace);
    
    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);
    
    List<WorkspaceMember> findByUserId(Long userId);
    
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    
    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    
    void deleteByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    
    @Query("SELECT m.userId FROM WorkspaceMember m WHERE m.workspace.id = :workspaceId")
    List<Long> findUserIdsByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
