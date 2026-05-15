package com.flowboard.board.controller;

import com.flowboard.board.dto.BoardRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.MemberRequest;
import com.flowboard.board.dto.MessageResponse;
import com.flowboard.board.model.BoardMember;
import com.flowboard.board.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.flowboard.board.client.AuthClient;
import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.model.Board;
import com.flowboard.board.repository.BoardRepository;
import org.springframework.web.client.RestTemplate;
import com.flowboard.board.repository.BoardMemberRepository;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "*")
public class BoardController {
    
    @Autowired
    private BoardService boardService;
    
    @Autowired
    private AuthClient authClient;

    @Autowired
    private WorkspaceClient workspaceClient;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private BoardMemberRepository memberRepo;

    @Autowired
    private BoardRepository boardRepo;
    
    /**
     * Create a new board
     */
    @PostMapping
    public ResponseEntity<?> createBoard(@RequestBody BoardRequest request,
                                         @RequestHeader("X-User-Id") Long userId,
                                         @RequestHeader("Authorization") String token) {
        try {
            BoardResponse response = boardService.createBoard(request, userId, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get board by ID
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<?> getBoard(@PathVariable Long boardId,
                                      @RequestHeader("X-User-Id") Long userId,
                                      @RequestHeader("Authorization") String token) {
        try {
            BoardResponse response = boardService.getBoardById(boardId, userId, token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all boards in a workspace
     */
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<?> getBoardsByWorkspace(@PathVariable Long workspaceId,
                                                  @RequestHeader("X-User-Id") Long userId,
                                                  @RequestHeader("Authorization") String token) {
        try {
            List<BoardResponse> boards = boardService.getBoardsByWorkspace(workspaceId, userId, token);
            return ResponseEntity.ok(boards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get boards where current user is member
     */
    @GetMapping("/my-boards")
    public ResponseEntity<?> getMyBoards(@RequestHeader("X-User-Id") Long userId) {
        try {
            List<BoardResponse> boards = boardService.getMyBoards(userId);
            return ResponseEntity.ok(boards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all public boards (No auth header required for initial view, but gateway might pass it)
     */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicBoards() {
        try {
            List<BoardResponse> boards = boardService.getPublicBoards();
            return ResponseEntity.ok(boards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update board
     */
    @PutMapping("/{boardId}")
    public ResponseEntity<?> updateBoard(@PathVariable Long boardId,
                                         @RequestBody BoardRequest request,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            BoardResponse response = boardService.updateBoard(boardId, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Close/Archive board
     */
    @PutMapping("/{boardId}/close")
    public ResponseEntity<?> closeBoard(@PathVariable Long boardId,
                                        @RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("Authorization") String token) {
        try {
            boardService.closeBoard(boardId, userId, token);
            return ResponseEntity.ok(new MessageResponse("Board closed successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Delete board permanently
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId,
                                         @RequestHeader("X-User-Id") Long userId,
                                         @RequestHeader("Authorization") String token) {
        try {
            boardService.deleteBoard(boardId, userId, token);
            return ResponseEntity.ok(new MessageResponse("Board deleted permanently", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Add member to board
     */
    @PostMapping("/{boardId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long boardId,
                                       @RequestBody MemberRequest request,
                                       @RequestHeader("X-User-Id") Long userId,
                                       @RequestHeader("Authorization") String token) {
        try {
            boardService.addMember(boardId, request, userId, token);
            return ResponseEntity.ok(new MessageResponse("Member added successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Remove member from board
     */
    @DeleteMapping("/{boardId}/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable Long boardId,
                                          @PathVariable Long memberId,
                                          @RequestHeader("X-User-Id") Long userId,
                                          @RequestHeader("Authorization") String token) {
        try {
            boardService.removeMember(boardId, memberId, userId, token);
            return ResponseEntity.ok(new MessageResponse("Member removed successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update member role
     */
    @PutMapping("/{boardId}/members/{memberId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long boardId,
                                              @PathVariable Long memberId,
                                              @RequestBody Map<String, String> payload,
                                              @RequestHeader("X-User-Id") Long userId,
                                              @RequestHeader("Authorization") String token) {
        try {
            String newRole = payload.get("role");
            boardService.updateMemberRole(boardId, memberId, newRole, userId, token);
            return ResponseEntity.ok(new MessageResponse("Role updated successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all board members
     */
    @GetMapping("/{boardId}/members")
    public ResponseEntity<?> getBoardMembers(@PathVariable Long boardId,
                                             @RequestHeader("X-User-Id") Long userId,
                                             @RequestHeader("Authorization") String token) {
        try {
            List<BoardMember> members = boardService.getBoardMembers(boardId, userId, token);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (BoardMember member : members) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", member.getUserId());
            map.put("role", member.getRole());
            
            // Get user details from Auth Service via Feign Client
            try {
                Map<String, Object> userMap = authClient.getUserById(member.getUserId(), token);
                String fullName = (String) userMap.get("fullName");
                String username = (String) userMap.get("username");
                
                if (fullName != null && !fullName.isEmpty()) {
                    map.put("userName", fullName);
                } else if (username != null && !username.isEmpty()) {
                    map.put("userName", username);
                } else {
                    map.put("userName", "User " + member.getUserId());
                }
                map.put("userEmail", userMap.getOrDefault("email", ""));
            } catch (Exception e) {
                map.put("userName", "User " + member.getUserId());
            }
            
            result.add(map);
        }
        return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all members of the workspace this board belongs to
     */
    @GetMapping("/{boardId}/workspace-members")
    public ResponseEntity<?> getBoardWorkspaceMembers(@PathVariable Long boardId,
                                                     @RequestHeader("Authorization") String token) {
        try {
            Board board = boardRepo.findById(boardId)
                    .orElseThrow(() -> new RuntimeException("Board not found"));
            
            List<Map<String, Object>> members = workspaceClient.getWorkspaceMembers(board.getWorkspaceId(), token);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{boardId}/analytics")
    public ResponseEntity<Map<String, Object>> getBoardAnalytics(@PathVariable Long boardId,
                                                                 @RequestHeader("X-User-Id") Long userId,
                                                                 @RequestHeader("Authorization") String token) {
        Map<String, Object> analytics = new HashMap<>();
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("Authorization", token);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
        
        try {
            // Card counts per list
            org.springframework.http.ResponseEntity<Object> listsResponse = restTemplate.exchange(
                "http://localhost:8084/api/lists/board/" + boardId, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                Object.class);
            analytics.put("listStats", listsResponse.getBody());
        } catch (Exception e) {
            analytics.put("listStats", "List service unavailable");
        }
        
        try {
            // Overdue cards
            org.springframework.http.ResponseEntity<Object> overdueResponse = restTemplate.exchange(
                "http://localhost:8085/api/cards/board/" + boardId + "/overdue", 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                Object.class);
            analytics.put("overdueCards", overdueResponse.getBody());
        } catch (Exception e) {
            analytics.put("overdueCards", "Card service unavailable");
        }
        
        try {
            // Total cards
            org.springframework.http.ResponseEntity<Object> cardsResponse = restTemplate.exchange(
                "http://localhost:8085/api/cards/board/" + boardId, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                Object.class);
            analytics.put("totalCards", cardsResponse.getBody());
        } catch (Exception e) {
            analytics.put("totalCards", 0);
        }
       
        analytics.put("memberCount", memberRepo.countByBoardId(boardId));
        
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalBoards() {
        return ResponseEntity.ok(boardService.getTotalCount());
    }

    // Quick error response helper
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
