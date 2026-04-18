package com.flowboard.list.controller;

import com.flowboard.list.dto.ListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.MessageResponse;
import com.flowboard.list.dto.ReorderRequest;
import com.flowboard.list.service.ListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lists")
@CrossOrigin(origins = "*")
public class ListController {
    
    @Autowired
    private ListService listService;
    
    /**
     * Create a new list
     */
    @PostMapping
    public ResponseEntity<?> createList(@RequestBody ListRequest request,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            ListResponse response = listService.createList(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all lists for a board
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<?> getListsByBoard(@PathVariable Long boardId,
                                             @RequestHeader("X-User-Id") Long userId,
                                             @RequestParam(defaultValue = "false") boolean includeArchived) {
        try {
            List<ListResponse> lists = listService.getListsByBoard(boardId, userId, includeArchived);
            return ResponseEntity.ok(lists);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get single list by ID
     */
    @GetMapping("/{listId}")
    public ResponseEntity<?> getListById(@PathVariable Long listId,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            ListResponse response = listService.getListById(listId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update list
     */
    @PutMapping("/{listId}")
    public ResponseEntity<?> updateList(@PathVariable Long listId,
                                        @RequestBody ListRequest request,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            ListResponse response = listService.updateList(listId, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Reorder lists (drag and drop)
     */
    @PutMapping("/board/{boardId}/reorder")
    public ResponseEntity<?> reorderLists(@PathVariable Long boardId,
                                          @RequestBody ReorderRequest request,
                                          @RequestHeader("X-User-Id") Long userId) {
        try {
            List<ListResponse> lists = listService.reorderLists(boardId, request, userId);
            return ResponseEntity.ok(lists);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Archive list
     */
    @PutMapping("/{listId}/archive")
    public ResponseEntity<?> archiveList(@PathVariable Long listId,
                                         @RequestHeader("X-User-Id") Long userId) {
        try {
            listService.archiveList(listId, userId);
            return ResponseEntity.ok(new MessageResponse("List archived successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Unarchive list
     */
    @PutMapping("/{listId}/unarchive")
    public ResponseEntity<?> unarchiveList(@PathVariable Long listId,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            listService.unarchiveList(listId, userId);
            return ResponseEntity.ok(new MessageResponse("List unarchived successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Delete list permanently
     */
    @DeleteMapping("/{listId}")
    public ResponseEntity<?> deleteList(@PathVariable Long listId,
                                        @RequestHeader("X-User-Id") Long userId) {
        try {
            listService.deleteList(listId, userId);
            return ResponseEntity.ok(new MessageResponse("List deleted permanently", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get archived lists
     */
    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<?> getArchivedLists(@PathVariable Long boardId,
                                              @RequestHeader("X-User-Id") Long userId) {
        try {
            List<ListResponse> lists = listService.getArchivedLists(boardId, userId);
            return ResponseEntity.ok(lists);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }
    
    // Quick and dirty error response
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
