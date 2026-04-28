package com.flowboard.workspace.repository;

import com.flowboard.workspace.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    
    List<Workspace> findByOwnerId(Long ownerId);
    
    List<Workspace> findByVisibility(String visibility);
    
    List<Workspace> findByOwnerIdAndIsActive(Long ownerId, Boolean isActive);
    
    boolean existsByNameAndOwnerId(String name, Long ownerId);
    
    @Query("SELECT w FROM Workspace w JOIN w.members m WHERE m.userId = :userId AND m.status = 'ACTIVE'")
    List<Workspace> findByMemberUserId(@Param("userId") Long userId);
    
    @Query("SELECT w FROM Workspace w JOIN w.members m WHERE m.userId = :userId AND m.status = 'PENDING'")
    List<Workspace> findPendingByMemberUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(w) FROM Workspace w WHERE w.ownerId = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);
}
