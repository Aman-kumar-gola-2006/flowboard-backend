package com.flowboard.comment.service;

import com.flowboard.comment.client.CardClient;
import com.flowboard.comment.dto.CommentRequest;
import com.flowboard.comment.dto.CommentResponse;
import com.flowboard.comment.model.Comment;
import com.flowboard.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceTest {

    @Mock private CommentRepository commentRepo;
    @Mock private CardClient cardClient;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CommentService commentService;

    private Comment testComment;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setId(1L);
        testComment.setCardId(100L);
        testComment.setAuthorId(1L);
        testComment.setContent("This is a test comment");
        testComment.setParentCommentId(null);
        testComment.setIsEdited(false);
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setUpdatedAt(LocalDateTime.now());
    }

    // ========== ADD COMMENT ==========

    @Test
    @DisplayName("AddComment - creates top-level comment successfully")
    void addComment_WithValidRequest_ShouldCreateComment() {
        CommentRequest request = new CommentRequest();
        request.setCardId(100L);
        request.setContent("Hello World");

        when(commentRepo.save(any(Comment.class))).thenReturn(testComment);

        CommentResponse result = commentService.addComment(request, 1L, "Bearer token");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(commentRepo).save(any(Comment.class));
    }

    @Test
    @DisplayName("AddComment - creates reply when parent comment exists and belongs to same card")
    void addComment_WithValidParent_ShouldCreateReply() {
        Comment parentComment = new Comment();
        parentComment.setId(5L);
        parentComment.setCardId(100L);
        parentComment.setAuthorId(2L);
        parentComment.setContent("Parent comment");

        CommentRequest request = new CommentRequest();
        request.setCardId(100L);
        request.setContent("Reply text");
        request.setParentCommentId(5L);

        when(commentRepo.findById(5L)).thenReturn(Optional.of(parentComment));
        when(commentRepo.save(any(Comment.class))).thenReturn(testComment);

        assertDoesNotThrow(() -> commentService.addComment(request, 1L, "Bearer token"));
        verify(commentRepo).save(any(Comment.class));
    }

    @Test
    @DisplayName("AddComment - throws when parent comment not found")
    void addComment_WithMissingParent_ShouldThrowException() {
        CommentRequest request = new CommentRequest();
        request.setCardId(100L);
        request.setContent("Reply");
        request.setParentCommentId(999L);

        when(commentRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> commentService.addComment(request, 1L, "token"));
    }

    @Test
    @DisplayName("AddComment - throws when parent comment belongs to different card")
    void addComment_WithParentOnDifferentCard_ShouldThrowException() {
        Comment parentComment = new Comment();
        parentComment.setId(5L);
        parentComment.setCardId(999L); // Different card!
        parentComment.setContent("Parent on different card");

        CommentRequest request = new CommentRequest();
        request.setCardId(100L);
        request.setContent("Reply");
        request.setParentCommentId(5L);

        when(commentRepo.findById(5L)).thenReturn(Optional.of(parentComment));

        assertThrows(RuntimeException.class,
                () -> commentService.addComment(request, 1L, "token"));
    }

    // ========== GET COMMENTS BY CARD ==========

    @Test
    @DisplayName("GetCommentsByCard - returns top-level comments with replies")
    void getCommentsByCard_ShouldReturnCommentsWithReplies() {
        when(commentRepo.findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(testComment));
        when(commentRepo.findByParentCommentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        List<CommentResponse> results = commentService.getCommentsByCard(100L, 1L);

        assertThat(results).hasSize(1);
        assertEquals("This is a test comment", results.get(0).getContent());
    }

    @Test
    @DisplayName("GetCommentsByCard - returns empty list when no comments")
    void getCommentsByCard_WhenNoComments_ShouldReturnEmptyList() {
        when(commentRepo.findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(100L))
                .thenReturn(List.of());

        List<CommentResponse> results = commentService.getCommentsByCard(100L, 1L);

        assertThat(results).isEmpty();
    }

    // ========== UPDATE COMMENT ==========

    @Test
    @DisplayName("UpdateComment - success when author updates own comment")
    void updateComment_WhenAuthor_ShouldUpdateContent() {
        when(commentRepo.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepo.save(any(Comment.class))).thenReturn(testComment);

        CommentResponse result = commentService.updateComment(1L, "Updated content", 1L);

        assertNotNull(result);
        assertEquals("Updated content", testComment.getContent());
        assertTrue(testComment.getIsEdited());
        verify(commentRepo).save(any(Comment.class));
    }

    @Test
    @DisplayName("UpdateComment - throws when user is not the author")
    void updateComment_WhenNotAuthor_ShouldThrowException() {
        when(commentRepo.findById(1L)).thenReturn(Optional.of(testComment));

        // User 2 tries to edit user 1's comment
        assertThrows(RuntimeException.class,
                () -> commentService.updateComment(1L, "Hacked content", 2L));
        verify(commentRepo, never()).save(any());
    }

    @Test
    @DisplayName("UpdateComment - throws when comment not found")
    void updateComment_WhenNotFound_ShouldThrowException() {
        when(commentRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> commentService.updateComment(999L, "content", 1L));
    }

    // ========== DELETE COMMENT ==========

    @Test
    @DisplayName("DeleteComment - success when author deletes own comment")
    void deleteComment_WhenAuthor_ShouldDeleteComment() {
        when(commentRepo.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepo.findByParentCommentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        doNothing().when(commentRepo).delete(testComment);

        assertDoesNotThrow(() -> commentService.deleteComment(1L, 1L));
        verify(commentRepo).delete(testComment);
    }

    @Test
    @DisplayName("DeleteComment - also deletes all replies")
    void deleteComment_WithReplies_ShouldDeleteRepliesAndComment() {
        Comment reply = new Comment();
        reply.setId(2L);
        reply.setCardId(100L);
        reply.setAuthorId(2L);
        reply.setContent("A reply");
        reply.setParentCommentId(1L);

        when(commentRepo.findById(1L)).thenReturn(Optional.of(testComment));
        when(commentRepo.findByParentCommentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(reply));
        doNothing().when(commentRepo).delete(reply);
        doNothing().when(commentRepo).delete(testComment);

        assertDoesNotThrow(() -> commentService.deleteComment(1L, 1L));

        verify(commentRepo).delete(reply);
        verify(commentRepo).delete(testComment);
    }

    @Test
    @DisplayName("DeleteComment - throws when user is not the author")
    void deleteComment_WhenNotAuthor_ShouldThrowException() {
        when(commentRepo.findById(1L)).thenReturn(Optional.of(testComment));

        assertThrows(RuntimeException.class, () -> commentService.deleteComment(1L, 2L));
        verify(commentRepo, never()).delete(any(Comment.class));
    }

    // ========== GET COMMENT COUNT ==========

    @Test
    @DisplayName("GetCommentCount - returns correct count")
    void getCommentCount_ShouldReturnCount() {
        when(commentRepo.countByCardId(100L)).thenReturn(5L);

        Long count = commentService.getCommentCount(100L, 1L);

        assertEquals(5L, count);
    }
}
