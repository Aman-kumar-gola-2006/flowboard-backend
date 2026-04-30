package com.flowboard.board.service;

import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.dto.BoardRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.MemberRequest;
import com.flowboard.board.model.Board;
import com.flowboard.board.model.BoardMember;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
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
@DisplayName("BoardService Unit Tests")
class BoardServiceTest {

    @Mock private BoardRepository boardRepo;
    @Mock private BoardMemberRepository memberRepo;
    @Mock private WorkspaceClient workspaceClient;

    @InjectMocks
    private BoardService boardService;

    private Board testBoard;
    private BoardMember adminMember;

    @BeforeEach
    void setUp() {
        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setWorkspaceId(10L);
        testBoard.setName("Test Board");
        testBoard.setDescription("A test board");
        testBoard.setBackgroundColor("#ffffff");
        testBoard.setVisibility("PRIVATE");
        testBoard.setCreatedBy(1L);
        testBoard.setIsClosed(false);
        testBoard.setCreatedAt(LocalDateTime.now());
        testBoard.setUpdatedAt(LocalDateTime.now());

        adminMember = new BoardMember();
        adminMember.setId(1L);
        adminMember.setBoard(testBoard);
        adminMember.setUserId(1L);
        adminMember.setRole("ADMIN");
        adminMember.setJoinedAt(LocalDateTime.now());
    }

    // ========== CREATE BOARD ==========

    @Test
    @DisplayName("CreateBoard - success when user is workspace member")
    void createBoard_WhenUserIsWorkspaceMember_ShouldSucceed() {
        BoardRequest request = new BoardRequest();
        request.setWorkspaceId(10L);
        request.setName("New Board");
        request.setDescription("A new board");
        request.setVisibility("PRIVATE");

        when(workspaceClient.checkMembership(10L, 1L)).thenReturn(true);
        when(boardRepo.existsByNameAndWorkspaceId("New Board", 10L)).thenReturn(false);
        when(boardRepo.save(any(Board.class))).thenReturn(testBoard);
        when(memberRepo.findByBoardId(any())).thenReturn(List.of(adminMember));

        BoardResponse result = boardService.createBoard(request, 1L);

        assertNotNull(result);
        assertEquals("Test Board", result.getName());
        verify(boardRepo).save(any(Board.class));
    }

    @Test
    @DisplayName("CreateBoard - fail when user is not workspace member")
    void createBoard_WhenUserNotWorkspaceMember_ShouldThrowException() {
        BoardRequest request = new BoardRequest();
        request.setWorkspaceId(10L);
        request.setName("New Board");

        when(workspaceClient.checkMembership(10L, 1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> boardService.createBoard(request, 1L));
        verify(boardRepo, never()).save(any());
    }

    @Test
    @DisplayName("CreateBoard - fail when board name already exists in workspace")
    void createBoard_WithDuplicateName_ShouldThrowException() {
        BoardRequest request = new BoardRequest();
        request.setWorkspaceId(10L);
        request.setName("Test Board");

        when(workspaceClient.checkMembership(10L, 1L)).thenReturn(true);
        when(boardRepo.existsByNameAndWorkspaceId("Test Board", 10L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> boardService.createBoard(request, 1L));
        verify(boardRepo, never()).save(any());
    }

    // ========== GET BOARD BY ID ==========

    @Test
    @DisplayName("GetBoardById - returns board when user is member of private board")
    void getBoardById_WhenUserIsMember_ShouldReturnBoard() {
        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.existsByBoardIdAndUserId(1L, 1L)).thenReturn(true);
        when(memberRepo.findByBoardId(1L)).thenReturn(List.of(adminMember));

        BoardResponse result = boardService.getBoardById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("GetBoardById - throws when board not found")
    void getBoardById_WithInvalidId_ShouldThrowException() {
        when(boardRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> boardService.getBoardById(999L, 1L));
    }

    @Test
    @DisplayName("GetBoardById - throws when user has no access to private board")
    void getBoardById_WhenUserNotMemberOfPrivateBoard_ShouldThrowException() {
        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.existsByBoardIdAndUserId(1L, 2L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> boardService.getBoardById(1L, 2L));
    }

    // ========== GET BOARDS BY WORKSPACE ==========

    @Test
    @DisplayName("GetBoardsByWorkspace - returns boards when user is workspace member")
    void getBoardsByWorkspace_WhenMember_ShouldReturnBoards() {
        when(workspaceClient.checkMembership(10L, 1L)).thenReturn(true);
        when(boardRepo.findByWorkspaceIdAndIsClosed(10L, false)).thenReturn(List.of(testBoard));
        when(memberRepo.existsByBoardIdAndUserId(1L, 1L)).thenReturn(true);
        when(memberRepo.findByBoardId(1L)).thenReturn(List.of(adminMember));

        List<BoardResponse> results = boardService.getBoardsByWorkspace(10L, 1L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("GetBoardsByWorkspace - throws when user not workspace member")
    void getBoardsByWorkspace_WhenNotMember_ShouldThrowException() {
        when(workspaceClient.checkMembership(10L, 1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> boardService.getBoardsByWorkspace(10L, 1L));
    }

    // ========== GET MY BOARDS ==========

    @Test
    @DisplayName("GetMyBoards - returns user's boards")
    void getMyBoards_ShouldReturnUserBoards() {
        when(boardRepo.findBoardsByMemberUserId(1L)).thenReturn(List.of(testBoard));
        when(memberRepo.findByBoardId(1L)).thenReturn(List.of(adminMember));

        List<BoardResponse> results = boardService.getMyBoards(1L);

        assertThat(results).hasSize(1);
    }

    // ========== UPDATE BOARD ==========

    @Test
    @DisplayName("UpdateBoard - success when user is board admin")
    void updateBoard_WhenAdmin_ShouldSucceed() {
        BoardRequest request = new BoardRequest();
        request.setName("Updated Name");

        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 1L)).thenReturn("ADMIN");
        when(boardRepo.save(any(Board.class))).thenReturn(testBoard);
        when(memberRepo.findByBoardId(1L)).thenReturn(List.of(adminMember));

        BoardResponse result = boardService.updateBoard(1L, request, 1L);

        assertNotNull(result);
        verify(boardRepo).save(any(Board.class));
    }

    @Test
    @DisplayName("UpdateBoard - throws when user is not admin")
    void updateBoard_WhenNotAdmin_ShouldThrowException() {
        BoardRequest request = new BoardRequest();
        request.setName("Updated Name");

        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 2L)).thenReturn("MEMBER");

        assertThrows(RuntimeException.class, () -> boardService.updateBoard(1L, request, 2L));
        verify(boardRepo, never()).save(any());
    }

    // ========== CLOSE BOARD ==========

    @Test
    @DisplayName("CloseBoard - success when user is board admin")
    void closeBoard_WhenBoardAdmin_ShouldCloseBoard() {
        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 1L)).thenReturn("ADMIN");
        when(boardRepo.save(any(Board.class))).thenReturn(testBoard);

        assertDoesNotThrow(() -> boardService.closeBoard(1L, 1L));
        assertTrue(testBoard.getIsClosed());
    }

    @Test
    @DisplayName("CloseBoard - success when user is workspace admin")
    void closeBoard_WhenWorkspaceAdmin_ShouldCloseBoard() {
        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 2L)).thenReturn("MEMBER");
        when(workspaceClient.isWorkspaceAdmin(10L, 2L)).thenReturn(true);
        when(boardRepo.save(any(Board.class))).thenReturn(testBoard);

        assertDoesNotThrow(() -> boardService.closeBoard(1L, 2L));
    }

    // ========== ADD MEMBER ==========

    @Test
    @DisplayName("AddMember - success when admin adds workspace member")
    void addMember_WhenAdminAndWorkspaceMember_ShouldSucceed() {
        MemberRequest request = new MemberRequest();
        request.setUserId(2L);
        request.setRole("MEMBER");

        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 1L)).thenReturn("ADMIN");
        when(workspaceClient.checkMembership(10L, 2L)).thenReturn(true);
        when(memberRepo.existsByBoardIdAndUserId(1L, 2L)).thenReturn(false);
        when(memberRepo.save(any(BoardMember.class))).thenReturn(adminMember);

        assertDoesNotThrow(() -> boardService.addMember(1L, request, 1L));
        verify(memberRepo).save(any(BoardMember.class));
    }

    @Test
    @DisplayName("AddMember - throws when user is already a member")
    void addMember_WhenAlreadyMember_ShouldThrowException() {
        MemberRequest request = new MemberRequest();
        request.setUserId(1L);

        when(boardRepo.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 1L)).thenReturn("ADMIN");
        when(workspaceClient.checkMembership(10L, 1L)).thenReturn(true);
        when(memberRepo.existsByBoardIdAndUserId(1L, 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> boardService.addMember(1L, request, 1L));
    }

    // ========== REMOVE MEMBER ==========

    @Test
    @DisplayName("RemoveMember - admin can remove another member")
    void removeMember_WhenAdmin_ShouldRemoveMember() {
        BoardMember regularMember = new BoardMember();
        regularMember.setUserId(2L);
        regularMember.setRole("MEMBER");

        when(memberRepo.findByBoardId(1L)).thenReturn(List.of(adminMember, regularMember));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 1L)).thenReturn("ADMIN");
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 2L)).thenReturn("MEMBER");
        doNothing().when(memberRepo).deleteByBoardIdAndUserId(1L, 2L);

        assertDoesNotThrow(() -> boardService.removeMember(1L, 2L, 1L));
        verify(memberRepo).deleteByBoardIdAndUserId(1L, 2L);
    }

    @Test
    @DisplayName("RemoveMember - throws when removing last admin")
    void removeMember_WhenLastAdmin_ShouldThrowException() {
        when(memberRepo.findByBoardId(1L)).thenReturn(List.of(adminMember));
        when(memberRepo.findRoleByBoardIdAndUserId(1L, 1L)).thenReturn("ADMIN");

        assertThrows(RuntimeException.class, () -> boardService.removeMember(1L, 1L, 1L));
    }

    // ========== GET TOTAL COUNT ==========

    @Test
    @DisplayName("GetTotalCount - returns correct count")
    void getTotalCount_ShouldReturnBoardCount() {
        when(boardRepo.count()).thenReturn(5L);
        assertEquals(5L, boardService.getTotalCount());
    }
}
