package com.flowboard.list.service;

import com.flowboard.list.client.BoardClient;
import com.flowboard.list.dto.ListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.ReorderRequest;
import com.flowboard.list.model.TaskList;
import com.flowboard.list.repository.ListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListService {
    
    private static final Logger log = LoggerFactory.getLogger(ListService.class);
    
    @Autowired
    private ListRepository listRepo;
    
    @Autowired
    private BoardClient boardClient;
    
    /**
     * Create a new list in a board.
     * Automatically assigns the next available position.
     */
    @Transactional
    public ListResponse createList(ListRequest request, Long userId) {
        
        log.info("Creating list '{}' for board {} by user {}", 
                 request.getName(), request.getBoardId(), userId);
        
        // Check if user has access to the board
        try {
            boardClient.getBoardById(request.getBoardId(), userId);
        } catch (Exception e) {
            log.error("Board access check failed for board {} and user {}: {}", 
                      request.getBoardId(), userId, e.getMessage());
            throw new RuntimeException("Board not found or access denied: " + e.getMessage());
        }
        
        // Find max position and add 1
        Integer maxPosition = listRepo.findMaxPositionByBoardId(request.getBoardId());
        int newPosition = (maxPosition != null) ? maxPosition + 1 : 0;
        
        TaskList list = new TaskList();
        list.setBoardId(request.getBoardId());
        list.setName(request.getName());
        list.setColor(request.getColor() != null ? request.getColor() : "#dddddd");
        list.setPosition(newPosition);
        list.setIsArchived(false);
        list.setCreatedAt(LocalDateTime.now());
        list.setUpdatedAt(LocalDateTime.now());
        
        TaskList saved = listRepo.save(list);
        log.info("List created with ID: {} at position: {}", saved.getId(), newPosition);
        
        return mapToResponse(saved);
    }
    
    /**
     * Get all active lists for a board, ordered by position.
     */
    public List<ListResponse> getListsByBoard(Long boardId, Long userId, boolean includeArchived) {
        
        log.info("Fetching lists for board: {}", boardId);
        
        // Check board access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            log.error("Failed to fetch lists for board {}: {}", boardId, e.getMessage());
            throw new RuntimeException("Board not found or access denied: " + e.getMessage());
        }
        
        List<TaskList> lists;
        if (includeArchived) {
            lists = listRepo.findByBoardIdOrderByPositionAsc(boardId);
        } else {
            lists = listRepo.findByBoardIdAndIsArchivedOrderByPositionAsc(boardId, false);
        }
        
        return lists.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a single list by ID.
     */
    public ListResponse getListById(Long listId, Long userId) {
        
        TaskList list = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found with id: " + listId));
        
        // Verify board access
        try {
            boardClient.getBoardById(list.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        return mapToResponse(list);
    }
    
    /**
     * Update list details (name, color).
     */
    @Transactional
    public ListResponse updateList(Long listId, ListRequest request, Long userId) {
        
        TaskList list = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        
        // Check access
        try {
            boardClient.getBoardById(list.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        if (request.getName() != null) {
            list.setName(request.getName());
        }
        if (request.getColor() != null) {
            list.setColor(request.getColor());
        }
        list.setUpdatedAt(LocalDateTime.now());
        
        TaskList updated = listRepo.save(list);
        return mapToResponse(updated);
    }
    
    /**
     * Reorder lists (drag and drop).
     * Updates position for all lists in the given order.
     */
    @Transactional
    public List<ListResponse> reorderLists(Long boardId, ReorderRequest request, Long userId) {
        
        log.info("Reordering lists for board: {}", boardId);
        
        // Check access
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        List<Long> orderedIds = request.getListIds();
        
        for (int i = 0; i < orderedIds.size(); i++) {
            Long listId = orderedIds.get(i);
            TaskList list = listRepo.findById(listId)
                    .orElseThrow(() -> new RuntimeException("List not found: " + listId));
            
            // Verify list belongs to this board
            if (!list.getBoardId().equals(boardId)) {
                throw new RuntimeException("List " + listId + " does not belong to board " + boardId);
            }
            
            list.setPosition(i);
            list.setUpdatedAt(LocalDateTime.now());
            listRepo.save(list);
        }
        
        log.info("Reordered {} lists", orderedIds.size());
        
        return getListsByBoard(boardId, userId, false);
    }
    
    /**
     * Archive a list (soft delete).
     */
    @Transactional
    public void archiveList(Long listId, Long userId) {
        
        TaskList list = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        
        try {
            boardClient.getBoardById(list.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        list.setIsArchived(true);
        list.setUpdatedAt(LocalDateTime.now());
        listRepo.save(list);
        
        log.info("List {} archived", listId);
    }
    
    /**
     * Unarchive a list.
     */
    @Transactional
    public void unarchiveList(Long listId, Long userId) {
        
        TaskList list = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        
        try {
            boardClient.getBoardById(list.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        list.setIsArchived(false);
        list.setUpdatedAt(LocalDateTime.now());
        listRepo.save(list);
        
        log.info("List {} unarchived", listId);
    }
    
    /**
     * Permanently delete a list.
     */
    @Transactional
    public void deleteList(Long listId, Long userId) {
        
        TaskList list = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        
        try {
            boardClient.getBoardById(list.getBoardId(), userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        listRepo.delete(list);
        log.warn("List {} permanently deleted", listId);
    }
    
    /**
     * Get archived lists for a board.
     */
    public List<ListResponse> getArchivedLists(Long boardId, Long userId) {
        
        try {
            boardClient.getBoardById(boardId, userId);
        } catch (Exception e) {
            throw new RuntimeException("Access denied");
        }
        
        List<TaskList> lists = listRepo.findByBoardIdAndIsArchivedTrue(boardId);
        return lists.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    // Helper method to map entity to response DTO
    private ListResponse mapToResponse(TaskList list) {
        return ListResponse.builder()
                .id(list.getId())
                .boardId(list.getBoardId())
                .name(list.getName())
                .position(list.getPosition())
                .color(list.getColor())
                .isArchived(list.getIsArchived())
                .createdAt(list.getCreatedAt())
                .cardCount(0) // Will update when Card Service is ready
                .build();
    }
}
