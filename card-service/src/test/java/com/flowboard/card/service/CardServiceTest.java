package com.flowboard.card.service;

import com.flowboard.card.client.ListClient;
import com.flowboard.card.dto.CardRequest;
import com.flowboard.card.dto.CardResponse;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.model.Card;
import com.flowboard.card.model.CardActivity;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService Unit Tests")
class CardServiceTest {

    @Mock private CardRepository cardRepo;
    @Mock private ListClient listClient;
    @Mock private CardActivityRepository activityRepo;
    // Needed because CardService now injects this for Redis caching
    @Mock private CardPositionCacheService cacheService;

    @InjectMocks
    private CardService cardService;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = new Card();
        testCard.setId(1L);
        testCard.setListId(10L);
        testCard.setBoardId(5L);
        testCard.setTitle("Test Card");
        testCard.setDescription("A test card");
        testCard.setPosition(0);
        testCard.setPriority(Priority.MEDIUM);
        testCard.setStatus(Status.TO_DO);
        testCard.setIsArchived(false);
        testCard.setCoverColor("#ffffff");
        testCard.setCreatedBy(1L);
        testCard.setCreatedAt(LocalDateTime.now());
        testCard.setUpdatedAt(LocalDateTime.now());
    }

    // ========== CREATE CARD ==========

    @Test
    @DisplayName("CreateCard - success when list is accessible")
    void createCard_WhenListAccessible_ShouldReturnCard() {
        CardRequest request = new CardRequest();
        request.setListId(10L);
        request.setBoardId(5L);
        request.setTitle("New Card");
        request.setPriority(Priority.HIGH);

        when(listClient.getListById(10L, 1L)).thenReturn(null); // success = no exception
        when(cardRepo.findMaxPositionByListId(10L)).thenReturn(null);
        when(cardRepo.save(any(Card.class))).thenReturn(testCard);
        when(activityRepo.save(any(CardActivity.class))).thenReturn(new CardActivity());

        CardResponse result = cardService.createCard(request, 1L);

        assertNotNull(result);
        assertEquals("Test Card", result.getTitle());
        verify(cardRepo).save(any(Card.class));
    }

    @Test
    @DisplayName("CreateCard - throws when list is not accessible")
    void createCard_WhenListNotAccessible_ShouldThrowException() {
        CardRequest request = new CardRequest();
        request.setListId(10L);
        request.setTitle("New Card");

        when(listClient.getListById(10L, 1L)).thenThrow(new RuntimeException("Not found"));

        assertThrows(RuntimeException.class, () -> cardService.createCard(request, 1L));
        verify(cardRepo, never()).save(any());
    }

    @Test
    @DisplayName("CreateCard - sets position to 0 when list is empty")
    void createCard_WhenListEmpty_ShouldSetPositionZero() {
        CardRequest request = new CardRequest();
        request.setListId(10L);
        request.setBoardId(5L);
        request.setTitle("First Card");

        when(listClient.getListById(10L, 1L)).thenReturn(null);
        when(cardRepo.findMaxPositionByListId(10L)).thenReturn(null);
        when(cardRepo.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            assertEquals(0, c.getPosition());
            return testCard;
        });
        when(activityRepo.save(any(CardActivity.class))).thenReturn(new CardActivity());

        cardService.createCard(request, 1L);
    }

    // ========== GET CARDS BY LIST ==========

    @Test
    @DisplayName("GetCardsByList - returns active cards")
    void getCardsByList_WhenAccessible_ShouldReturnCards() {
        when(listClient.getListById(10L, 1L)).thenReturn(null);
        when(cardRepo.findByListIdAndIsArchivedOrderByPositionAsc(10L, false))
                .thenReturn(List.of(testCard));

        List<CardResponse> results = cardService.getCardsByList(10L, 1L);

        assertThat(results).hasSize(1);
        assertEquals("Test Card", results.get(0).getTitle());
    }

    @Test
    @DisplayName("GetCardsByList - throws when list not accessible")
    void getCardsByList_WhenListNotAccessible_ShouldThrowException() {
        when(listClient.getListById(10L, 1L)).thenThrow(new RuntimeException("Access denied"));

        assertThrows(RuntimeException.class, () -> cardService.getCardsByList(10L, 1L));
    }

    // ========== GET CARD BY ID ==========

    @Test
    @DisplayName("GetCardById - returns card when found and accessible")
    void getCardById_WhenFoundAndAccessible_ShouldReturnCard() {
        when(cardRepo.findById(1L)).thenReturn(Optional.of(testCard));
        when(listClient.getListById(10L, 1L)).thenReturn(null);

        CardResponse result = cardService.getCardById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("GetCardById - throws when card not found")
    void getCardById_WhenNotFound_ShouldThrowException() {
        when(cardRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cardService.getCardById(999L, 1L));
    }

    // ========== UPDATE CARD ==========

    @Test
    @DisplayName("UpdateCard - success when accessible")
    void updateCard_WhenAccessible_ShouldUpdateCard() {
        CardRequest request = new CardRequest();
        request.setTitle("Updated Title");
        request.setPriority(Priority.HIGH);

        when(cardRepo.findById(1L)).thenReturn(Optional.of(testCard));
        when(listClient.getListById(10L, 1L)).thenReturn(null);
        when(cardRepo.save(any(Card.class))).thenReturn(testCard);
        when(activityRepo.save(any(CardActivity.class))).thenReturn(new CardActivity());

        CardResponse result = cardService.updateCard(1L, request, 1L);

        assertNotNull(result);
        verify(cardRepo).save(any(Card.class));
    }

    // ========== UPDATE CARD STATUS ==========

    @Test
    @DisplayName("UpdateCardStatus - changes status successfully")
    void updateCardStatus_ShouldChangeStatus() {
        when(cardRepo.findById(1L)).thenReturn(Optional.of(testCard));
        when(listClient.getListById(10L, 1L)).thenReturn(null);
        when(cardRepo.save(any(Card.class))).thenReturn(testCard);
        when(activityRepo.save(any(CardActivity.class))).thenReturn(new CardActivity());

        assertDoesNotThrow(() -> cardService.updateCardStatus(1L, Status.IN_PROGRESS, 1L));

        assertEquals(Status.IN_PROGRESS, testCard.getStatus());
    }

    // ========== ARCHIVE CARD ==========

    @Test
    @DisplayName("ArchiveCard - sets isArchived to true")
    void archiveCard_ShouldSetArchivedTrue() {
        when(cardRepo.findById(1L)).thenReturn(Optional.of(testCard));
        when(listClient.getListById(10L, 1L)).thenReturn(null);
        when(cardRepo.save(any(Card.class))).thenReturn(testCard);

        assertDoesNotThrow(() -> cardService.archiveCard(1L, 1L));
        assertTrue(testCard.getIsArchived());
    }

    // ========== DELETE CARD ==========

    @Test
    @DisplayName("DeleteCard - deletes card when accessible")
    void deleteCard_WhenAccessible_ShouldDelete() {
        when(cardRepo.findById(1L)).thenReturn(Optional.of(testCard));
        when(listClient.getListById(10L, 1L)).thenReturn(null);
        doNothing().when(cardRepo).delete(testCard);

        assertDoesNotThrow(() -> cardService.deleteCard(1L, 1L));
        verify(cardRepo).delete(testCard);
    }

    // ========== GET CARDS BY BOARD ==========

    @Test
    @DisplayName("GetCardsByBoard - returns non-archived cards")
    void getCardsByBoard_ShouldReturnNonArchivedCards() {
        when(cardRepo.findByBoardIdAndIsArchivedFalse(5L)).thenReturn(List.of(testCard));

        List<CardResponse> results = cardService.getCardsByBoard(5L, 1L);

        assertThat(results).hasSize(1);
    }

    // ========== GET CARDS BY ASSIGNEE ==========

    @Test
    @DisplayName("GetCardsByAssignee - returns non-archived assigned cards")
    void getCardsByAssignee_ShouldReturnAssignedCards() {
        testCard.setAssigneeId(1L);
        when(cardRepo.findByAssigneeId(1L)).thenReturn(List.of(testCard));

        List<CardResponse> results = cardService.getCardsByAssignee(1L, 1L);

        assertThat(results).hasSize(1);
    }

    // ========== GET TOTAL COUNT ==========

    @Test
    @DisplayName("GetTotalCount - returns correct count")
    void getTotalCount_ShouldReturnCount() {
        when(cardRepo.count()).thenReturn(10L);
        assertEquals(10L, cardService.getTotalCount());
    }
}
