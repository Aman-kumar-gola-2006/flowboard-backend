package com.flowboard.list.service;

import com.flowboard.list.client.BoardClient;
import com.flowboard.list.dto.ListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.ReorderRequest;
import com.flowboard.list.model.TaskList;
import com.flowboard.list.repository.ListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListService Unit Tests")
class ListServiceTest {

    @Mock private ListRepository listRepo;
    @Mock private BoardClient boardClient;

    @InjectMocks
    private ListService listService;

    private TaskList testList;

    @BeforeEach
    void setUp() {
        testList = new TaskList();
        testList.setId(1L);
        testList.setBoardId(10L);
        testList.setName("To Do");
        testList.setColor("#dddddd");
        testList.setPosition(0);
        testList.setIsArchived(false);
        testList.setCreatedAt(LocalDateTime.now());
        testList.setUpdatedAt(LocalDateTime.now());
    }

    // ========== CREATE LIST ==========

    @Test
    @DisplayName("CreateList - success when board is accessible")
    void createList_WhenBoardAccessible_ShouldReturnListResponse() {
        ListRequest request = new ListRequest();
        request.setBoardId(10L);
        request.setName("In Progress");

        when(boardClient.getBoardById(10L, 1L)).thenReturn(null); // no exception = success
        when(listRepo.findMaxPositionByBoardId(10L)).thenReturn(null);
        when(listRepo.save(any(TaskList.class))).thenReturn(testList);

        ListResponse result = listService.createList(request, 1L);

        assertNotNull(result);
        assertEquals("To Do", result.getName());
        verify(listRepo).save(any(TaskList.class));
    }

    @Test
    @DisplayName("CreateList - sets position 0 when board has no lists")
    void createList_WhenFirstList_ShouldSetPositionZero() {
        ListRequest request = new ListRequest();
        request.setBoardId(10L);
        request.setName("First List");

        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.findMaxPositionByBoardId(10L)).thenReturn(null);
        when(listRepo.save(any(TaskList.class))).thenAnswer(inv -> {
            TaskList list = inv.getArgument(0);
            assertEquals(0, list.getPosition());
            return testList;
        });

        listService.createList(request, 1L);
    }

    @Test
    @DisplayName("CreateList - sets next position when board already has lists")
    void createList_WhenListsExist_ShouldIncrementPosition() {
        ListRequest request = new ListRequest();
        request.setBoardId(10L);
        request.setName("Done");

        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.findMaxPositionByBoardId(10L)).thenReturn(2);
        when(listRepo.save(any(TaskList.class))).thenAnswer(inv -> {
            TaskList list = inv.getArgument(0);
            assertEquals(3, list.getPosition());
            return testList;
        });

        listService.createList(request, 1L);
    }

    @Test
    @DisplayName("CreateList - throws when board access denied")
    void createList_WhenBoardNotAccessible_ShouldThrowException() {
        ListRequest request = new ListRequest();
        request.setBoardId(10L);
        request.setName("Test");

        when(boardClient.getBoardById(10L, 1L)).thenThrow(new RuntimeException("Access denied"));

        assertThrows(RuntimeException.class, () -> listService.createList(request, 1L));
        verify(listRepo, never()).save(any());
    }

    // ========== GET LISTS BY BOARD ==========

    @Test
    @DisplayName("GetListsByBoard - returns active lists when not including archived")
    void getListsByBoard_ShouldReturnActiveLists() {
        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.findByBoardIdAndIsArchivedOrderByPositionAsc(10L, false))
                .thenReturn(List.of(testList));

        List<ListResponse> results = listService.getListsByBoard(10L, 1L, false);

        assertThat(results).hasSize(1);
        assertEquals("To Do", results.get(0).getName());
    }

    @Test
    @DisplayName("GetListsByBoard - returns all lists when includeArchived=true")
    void getListsByBoard_WithIncludeArchived_ShouldReturnAllLists() {
        TaskList archived = new TaskList();
        archived.setId(2L);
        archived.setName("Done");
        archived.setIsArchived(true);
        archived.setBoardId(10L);
        archived.setPosition(1);
        archived.setColor("#ccc");

        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.findByBoardIdOrderByPositionAsc(10L)).thenReturn(List.of(testList, archived));

        List<ListResponse> results = listService.getListsByBoard(10L, 1L, true);

        assertThat(results).hasSize(2);
    }

    // ========== GET LIST BY ID ==========

    @Test
    @DisplayName("GetListById - returns list when found and board accessible")
    void getListById_WhenFoundAndAccessible_ShouldReturnList() {
        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);

        ListResponse result = listService.getListById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("To Do", result.getName());
    }

    @Test
    @DisplayName("GetListById - throws when list not found")
    void getListById_WhenNotFound_ShouldThrowException() {
        when(listRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> listService.getListById(999L, 1L));
    }

    @Test
    @DisplayName("GetListById - throws when board access denied")
    void getListById_WhenBoardAccessDenied_ShouldThrowException() {
        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(boardClient.getBoardById(10L, 1L)).thenThrow(new RuntimeException("Denied"));

        assertThrows(RuntimeException.class, () -> listService.getListById(1L, 1L));
    }

    // ========== UPDATE LIST ==========

    @Test
    @DisplayName("UpdateList - success when board accessible")
    void updateList_WhenAccessible_ShouldUpdateList() {
        ListRequest request = new ListRequest();
        request.setName("In Progress");

        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.save(any(TaskList.class))).thenReturn(testList);

        ListResponse result = listService.updateList(1L, request, 1L);

        assertNotNull(result);
        assertEquals("In Progress", testList.getName());
    }

    // ========== ARCHIVE / UNARCHIVE ==========

    @Test
    @DisplayName("ArchiveList - sets isArchived to true")
    void archiveList_ShouldSetArchivedTrue() {
        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.save(any(TaskList.class))).thenReturn(testList);

        assertDoesNotThrow(() -> listService.archiveList(1L, 1L));
        assertTrue(testList.getIsArchived());
    }

    @Test
    @DisplayName("UnarchiveList - sets isArchived to false")
    void unarchiveList_ShouldSetArchivedFalse() {
        testList.setIsArchived(true);

        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.save(any(TaskList.class))).thenReturn(testList);

        assertDoesNotThrow(() -> listService.unarchiveList(1L, 1L));
        assertFalse(testList.getIsArchived());
    }

    // ========== DELETE LIST ==========

    @Test
    @DisplayName("DeleteList - deletes when accessible")
    void deleteList_WhenAccessible_ShouldDelete() {
        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        doNothing().when(listRepo).delete(testList);

        assertDoesNotThrow(() -> listService.deleteList(1L, 1L));
        verify(listRepo).delete(testList);
    }

    // ========== REORDER LISTS ==========

    @Test
    @DisplayName("ReorderLists - updates positions in given order")
    void reorderLists_ShouldUpdatePositions() {
        TaskList list2 = new TaskList();
        list2.setId(2L);
        list2.setBoardId(10L);
        list2.setName("In Progress");
        list2.setColor("#aaa");
        list2.setIsArchived(false);
        list2.setPosition(0);

        ReorderRequest request = new ReorderRequest();
        request.setListIds(List.of(2L, 1L)); // Swap order

        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.findById(2L)).thenReturn(Optional.of(list2));
        when(listRepo.findById(1L)).thenReturn(Optional.of(testList));
        when(listRepo.save(any(TaskList.class))).thenReturn(testList);
        when(listRepo.findByBoardIdAndIsArchivedOrderByPositionAsc(10L, false))
                .thenReturn(List.of(list2, testList));

        List<ListResponse> results = listService.reorderLists(10L, request, 1L);

        assertThat(results).hasSize(2);
        verify(listRepo, times(2)).save(any(TaskList.class));
    }

    @Test
    @DisplayName("ReorderLists - throws when list doesn't belong to board")
    void reorderLists_WhenListBelongsToDifferentBoard_ShouldThrowException() {
        TaskList foreignList = new TaskList();
        foreignList.setId(2L);
        foreignList.setBoardId(999L); // Different board!
        foreignList.setName("Foreign");

        ReorderRequest request = new ReorderRequest();
        request.setListIds(List.of(2L));

        when(boardClient.getBoardById(10L, 1L)).thenReturn(null);
        when(listRepo.findById(2L)).thenReturn(Optional.of(foreignList));

        assertThrows(RuntimeException.class, () -> listService.reorderLists(10L, request, 1L));
    }
}
