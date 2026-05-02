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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestTemplate;
import com.flowboard.board.repository.BoardMemberRepository;

@RestController
@RequestMapping("/api/boards")
public class BoardController {
    
    @Autowired
    private BoardService boardService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private BoardMemberRepository memberRepo;
    
    /**
     * Create a new board
     */
    @PostMapping
    public ResponseEntity<?> createBoard(@RequestBody BoardRequest request,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            BoardResponse response = boardService.createBoard(request, userId);
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
                                      @RequestHeader("X-User-Id") Long userId) {
        try {
            BoardResponse response = boardService.getBoardById(boardId, userId);
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
                                                  @RequestHeader("X-User-Id") Long userId) {
        try {
            List<BoardResponse> boards = boardService.getBoardsByWorkspace(workspaceId, userId);
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
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            boardService.closeBoard(boardId, userId);
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
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            boardService.deleteBoard(boardId, userId);
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
                                       @RequestHeader("X-User-Id") Long userId) {
        try {
            boardService.addMember(boardId, request, userId);
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
                                          @RequestHeader("X-User-Id") Long userId) {
        try {
            boardService.removeMember(boardId, memberId, userId);
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
                                              @RequestHeader("X-User-Id") Long userId) {
        try {
            String newRole = payload.get("role");
            boardService.updateMemberRole(boardId, memberId, newRole, userId);
            return ResponseEntity.ok(new MessageResponse("Role updated successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all board members
     */
    @GetMapping("/{boardId}/members")
    public ResponseEntity<List<Map<String, Object>>> getBoardMembers(@PathVariable Long boardId) {
        List<BoardMember> members = memberRepo.findByBoardId(boardId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (BoardMember member : members) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", member.getUserId());
            map.put("role", member.getRole());
            
            // Get user details from Auth Service
            try {
                Object user = restTemplate.getForObject("http://localhost:8081/api/auth/users/" + member.getUserId(), Object.class);
                Map<String, Object> userMap = (Map<String, Object>) user;
                map.put("userName", userMap.getOrDefault("fullName", "User " + member.getUserId()));
                map.put("userEmail", userMap.getOrDefault("email", ""));
            } catch (Exception e) {
                map.put("userName", "User " + member.getUserId());
            }
            
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{boardId}/analytics")
    public ResponseEntity<Map<String, Object>> getBoardAnalytics(@PathVariable Long boardId) {
        Map<String, Object> analytics = new HashMap<>();
        
        try {
            // Card counts per list
            Object listsResponse = restTemplate.getForObject("http://localhost:8084/api/lists/board/" + boardId, Object.class);
            analytics.put("listStats", listsResponse);
        } catch (Exception e) {
            analytics.put("listStats", "List service unavailable");
        }
        
        try {
            // Overdue cards
            Object overdueResponse = restTemplate.getForObject("http://localhost:8085/api/cards/board/" + boardId + "/overdue", Object.class);
            analytics.put("overdueCards", overdueResponse);
        } catch (Exception e) {
            analytics.put("overdueCards", "Card service unavailable");
        }
        
        try {
            // Total cards
            Object cardsResponse = restTemplate.getForObject("http://localhost:8085/api/cards/board/" + boardId, Object.class);
            analytics.put("totalCards", cardsResponse);
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
