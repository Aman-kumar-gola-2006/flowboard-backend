package com.flowboard.board.service;

import com.flowboard.board.client.*;
import com.flowboard.board.dto.*;
import com.flowboard.board.model.Board;
import com.flowboard.board.model.BoardMember;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoardService {
    
    // Field injection - I know constructor is better but this is faster
    @Autowired
    private BoardRepository boardRepo;
    
    @Autowired
    private BoardMemberRepository memberRepo;
    
    @Autowired
    private WorkspaceClient workspaceClient;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private MessageProducer messageProducer;
    
    private static final Logger log = LoggerFactory.getLogger(BoardService.class);
    
    /**
     * Create a new board inside a workspace.
     * Checks if user is workspace member first.
     */
    @Transactional
    public BoardResponse createBoard(BoardRequest request, Long userId, String authToken) {
        
        log.info("Creating board '{}' for user {}", request.getName(), userId);
        
        // First check - is user part of this workspace?
        Boolean isMember = workspaceClient.checkMembership(request.getWorkspaceId(), userId, authToken);
        
        if (isMember == null || !isMember) {
            System.out.println("WARNING: User " + userId + " tried to create board without workspace access!");
            throw new RuntimeException("You are not a member of this workspace. Access denied.");
        }
        
        // Check if board name already exists
        if (boardRepo.existsByNameAndWorkspaceId(request.getName(), request.getWorkspaceId())) {
            throw new RuntimeException("Board with name '" + request.getName() + "' already exists in this workspace");
        }
        
        // Create board object
        Board board = new Board();
        board.setWorkspaceId(request.getWorkspaceId());
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        board.setBackgroundColor(request.getBackgroundColor() != null ? request.getBackgroundColor() : "#ffffff");
        board.setVisibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE");
        board.setCreatedBy(userId);
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(LocalDateTime.now());
        
        // Creator becomes board ADMIN by default
        board.addMember(userId, "ADMIN");
        
        Board saved = boardRepo.save(board);
        log.info("Board created successfully with ID: {}", saved.getId());
        
        return mapToResponse(saved);
    }
    
    /**
     * Get a single board by ID
     */
    public BoardResponse getBoardById(Long boardId, Long userId, String authToken) {
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        // Check if user has access to this board
        if (!hasAccess(board, userId, authToken)) {
            throw new RuntimeException("You don't have permission to view this board");
        }
        
        return mapToResponse(board);
    }
    
    /**
     * Get all boards in a workspace that user has access to
     */
    public List<BoardResponse> getBoardsByWorkspace(Long workspaceId, Long userId, String authToken) {
        
        // Check workspace membership first
        Boolean isMember = workspaceClient.checkMembership(workspaceId, userId, authToken);
        
        if (isMember == null || !isMember) {
            throw new RuntimeException("You are not a member of this workspace");
        }
        
        List<Board> boards = boardRepo.findByWorkspaceIdAndIsClosed(workspaceId, false);
        
        // Filter boards based on visibility and membership
        return boards.stream()
                .filter(board -> hasAccess(board, userId, authToken))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get boards where user is a member
     */
    public List<BoardResponse> getMyBoards(Long userId) {
        List<Board> boards = boardRepo.findBoardsByMemberUserId(userId);
        return boards.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all public boards that are not closed
     */
    public List<BoardResponse> getPublicBoards() {
        return boardRepo.findByVisibilityAndIsClosed("PUBLIC", false)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Update board details
     */
    @Transactional
    public BoardResponse updateBoard(Long boardId, BoardRequest request, Long userId) {
        
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Check if user is board ADMIN
        if (!isBoardAdmin(boardId, userId)) {
            throw new RuntimeException("Only board admin can update board details");
        }
        
        if (request.getName() != null) {
            board.setName(request.getName());
        }
        if (request.getDescription() != null) {
            board.setDescription(request.getDescription());
        }
        if (request.getBackgroundColor() != null) {
            board.setBackgroundColor(request.getBackgroundColor());
        }
        if (request.getVisibility() != null) {
            board.setVisibility(request.getVisibility());
        }
        
        board.setUpdatedAt(LocalDateTime.now());
        
        Board updated = boardRepo.save(board);
        log.info("Board {} updated by user {}", boardId, userId);
        
        return mapToResponse(updated);
    }
    
    /**
     * Close/Archive a board
     */
    @Transactional
    public void closeBoard(Long boardId, Long userId, String authToken) {
        
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Check if user is board ADMIN or workspace ADMIN
        boolean isBoardAdmin = isBoardAdmin(boardId, userId);
        boolean isWorkspaceAdmin = workspaceClient.isWorkspaceAdmin(board.getWorkspaceId(), userId, authToken);
        
        if (!isBoardAdmin && !isWorkspaceAdmin) {
            throw new RuntimeException("You don't have permission to close this board");
        }
        
        board.setIsClosed(true);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepo.save(board);
        
        log.info("Board {} closed by user {}", boardId, userId);
    }
    
    /**
     * Permanently delete board
     */
    @Transactional
    public void deleteBoard(Long boardId, Long userId, String authToken) {
        
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Only workspace admin can delete
        boolean isWorkspaceAdmin = workspaceClient.isWorkspaceAdmin(board.getWorkspaceId(), userId, authToken);
        
        if (!isWorkspaceAdmin) {
            throw new RuntimeException("Only workspace admin can permanently delete a board");
        }
        
        boardRepo.delete(board);
        log.warn("Board {} permanently deleted by user {}", boardId, userId);
    }
    
    /**
     * Add a member to board
     */
    @Transactional
    public void addMember(Long boardId, MemberRequest request, Long addedBy, String authToken) {
        
        Board board = boardRepo.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Check if adder is board admin
        if (!isBoardAdmin(boardId, addedBy)) {
            throw new RuntimeException("Only board admin can add members");
        }
        
        // Check if user is workspace member
        Boolean isWorkspaceMember = workspaceClient.checkMembership(board.getWorkspaceId(), request.getUserId(), authToken);
        
        if (isWorkspaceMember == null || !isWorkspaceMember) {
            throw new RuntimeException("User is not a member of this workspace");
        }
        
        // Check if already member
        if (memberRepo.existsByBoardIdAndUserId(boardId, request.getUserId())) {
            throw new RuntimeException("User is already a member of this board");
        }
        
        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUserId(request.getUserId());
        member.setRole(request.getRole() != null ? request.getRole() : "MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        
        memberRepo.save(member);
        log.info("User {} added to board {} as {}", request.getUserId(), boardId, member.getRole());
        
        // Send notification
        sendInvitationNotification(board, request.getUserId(), addedBy, authToken);
    }
    
    /**
     * Remove member from board
     */
    @Transactional
    public void removeMember(Long boardId, Long userId, Long removedBy, String authToken) {
        
        // Only admin can remove, or user can remove themselves
        if (!userId.equals(removedBy) && !isBoardAdmin(boardId, removedBy)) {
            throw new RuntimeException("You don't have permission to remove this member");
        }
        
        // Don't remove the last admin
        long adminCount = memberRepo.findByBoardId(boardId).stream()
                .filter(m -> "ADMIN".equals(m.getRole()))
                .count();
        
        String userRole = memberRepo.findRoleByBoardIdAndUserId(boardId, userId);
        
        if ("ADMIN".equals(userRole) && adminCount <= 1) {
            throw new RuntimeException("Cannot remove the last admin of the board");
        }
        
        memberRepo.deleteByBoardIdAndUserId(boardId, userId);
        log.info("User {} removed from board {}", userId, boardId);
    }
    
    /**
     * Update member role
     */
    @Transactional
    public void updateMemberRole(Long boardId, Long userId, String newRole, Long updatedBy, String authToken) {
        
        if (!isBoardAdmin(boardId, updatedBy)) {
            throw new RuntimeException("Only board admin can change roles");
        }
        
        BoardMember member = memberRepo.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        member.setRole(newRole);
        memberRepo.save(member);
        
        log.info("User {} role updated to {} in board {}", userId, newRole, boardId);
    }
    
    /**
     * Get all board members
     */
    public List<BoardMember> getBoardMembers(Long boardId, Long userId, String authToken) {
        
        if (!hasAccess(boardId, userId, authToken)) {
            throw new RuntimeException("You don't have access to this board");
        }
        
        return memberRepo.findByBoardId(boardId);
    }
    
    // ---------- HELPER METHODS ----------
    
    private void sendInvitationNotification(Board board, Long userId, Long inviterId, String authToken) {
        try {
            Map<String, Object> assignee = authClient.getUserById(userId, authToken);
            Map<String, Object> inviter = authClient.getUserById(inviterId, authToken);
            Map<String, Object> workspace = workspaceClient.getWorkspaceById(board.getWorkspaceId(), authToken);
            
            NotificationMessage msg = new NotificationMessage();
            msg.setEmail(assignee.get("email").toString());
            msg.setName(assignee.get("fullName").toString());
            msg.setType("INVITE");
            msg.setInviterName(inviter.get("fullName").toString());
            msg.setBoardName(board.getName());
            msg.setWorkspaceName(workspace.get("name").toString());
            msg.setRecipientId(userId);
            msg.setActorId(inviterId);
            msg.setExtraData("http://3.110.61.209.nip.io:4200/board/" + board.getId());

            messageProducer.sendNotification(msg);
            log.info("Board invitation notification sent to {}", msg.getEmail());
        } catch (Exception e) {
            log.error("Failed to send board invitation notification: {}", e.getMessage());
        }
    }
    
    private boolean hasAccess(Board board, Long userId, String authToken) {
        // Public boards are visible to all workspace members
        if ("PUBLIC".equals(board.getVisibility())) {
            return workspaceClient.checkMembership(board.getWorkspaceId(), userId, authToken);
        }
        // Private boards - only members can see
        return memberRepo.existsByBoardIdAndUserId(board.getId(), userId);
    }
    
    private boolean hasAccess(Long boardId, Long userId, String authToken) {
        Board board = boardRepo.findById(boardId).orElse(null);
        return board != null && hasAccess(board, userId, authToken);
    }
    
    private boolean isBoardAdmin(Long boardId, Long userId) {
        String role = memberRepo.findRoleByBoardIdAndUserId(boardId, userId);
        return "ADMIN".equals(role);
    }

    public long getTotalCount() {
        return boardRepo.count();
    }
    
    private BoardResponse mapToResponse(Board board) {
        int memberCount = memberRepo.findByBoardId(board.getId()).size();
        
        return BoardResponse.builder()
                .id(board.getId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .description(board.getDescription())
                .backgroundColor(board.getBackgroundColor())
                .visibility(board.getVisibility())
                .createdBy(board.getCreatedBy())
                .isClosed(board.getIsClosed())
                .createdAt(board.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }
}
