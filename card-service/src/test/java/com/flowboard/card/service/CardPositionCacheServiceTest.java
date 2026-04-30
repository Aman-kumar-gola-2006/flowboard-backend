package com.flowboard.card.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CardPositionCacheService.
 *
 * We mock RedisTemplate so no real Redis instance is needed.
 * All edge cases (null results, exceptions) are covered to make sure
 * the service degrades gracefully when Redis has problems.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CardPositionCacheService Unit Tests")
class CardPositionCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    // Operations sub-mocks - Redis template delegates to these
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CardPositionCacheService cacheService;

    @BeforeEach
    void setUp() {
        // Wire up the operations mocks - the service calls opsForHash() / opsForValue()
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ====================================================================
    // CACHE CARD POSITION
    // ====================================================================

    @Test
    @DisplayName("CacheCardPosition - stores position in Redis Hash and sets TTL")
    void cacheCardPosition_ShouldStoreInHashAndSetExpiry() {
        // No exception = success for RedisTemplate calls
        doNothing().when(hashOperations).put(anyString(), anyString(), any());
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Should not throw anything
        assertDoesNotThrow(() -> cacheService.cacheCardPosition(1L, 10L, 0));

        // Verify the Hash put was called with correct key pattern
        verify(hashOperations).put(eq("card:positions:10"), eq("1"), eq(0));

        // Verify the card->list mapping was stored
        verify(valueOperations).set(eq("card:list:1"), eq("10"), anyLong(), eq(TimeUnit.MINUTES));

        // Verify TTL was set on the position hash
        verify(redisTemplate).expire(eq("card:positions:10"), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("CacheCardPosition - does NOT throw when Redis is unavailable")
    void cacheCardPosition_WhenRedisFails_ShouldNotThrow() {
        // Simulate Redis being down
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis connection refused"));

        // The service should swallow the exception gracefully - Redis failure is non-fatal
        assertDoesNotThrow(() -> cacheService.cacheCardPosition(1L, 10L, 2));
    }

    // ====================================================================
    // GET CARD POSITION
    // ====================================================================

    @Test
    @DisplayName("GetCardPosition - returns cached position when data is in Redis")
    void getCardPosition_WhenCached_ShouldReturnPosition() {
        // card:list:1 → "10"  (card 1 is in list 10)
        when(valueOperations.get("card:list:1")).thenReturn("10");

        // card:positions:10 → { "1": 3 }
        when(hashOperations.get("card:positions:10", "1")).thenReturn(3);

        Integer result = cacheService.getCardPosition(1L);

        assertNotNull(result);
        assertEquals(3, result);
    }

    @Test
    @DisplayName("GetCardPosition - returns null on cache miss (no list mapping)")
    void getCardPosition_WhenNoListMapping_ShouldReturnNull() {
        // card:list:99 doesn't exist
        when(valueOperations.get("card:list:99")).thenReturn(null);

        Integer result = cacheService.getCardPosition(99L);

        assertNull(result);
        // Should NOT attempt hash lookup if list mapping is missing
        verify(hashOperations, never()).get(anyString(), anyString());
    }

    @Test
    @DisplayName("GetCardPosition - returns null when Redis throws exception")
    void getCardPosition_WhenRedisFails_ShouldReturnNull() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis timeout"));

        // Non-fatal - should return null so caller falls back to DB
        Integer result = cacheService.getCardPosition(1L);

        assertNull(result);
    }

    @Test
    @DisplayName("GetCardPosition - returns null when position hash entry missing")
    void getCardPosition_WhenPositionNotInHash_ShouldReturnNull() {
        when(valueOperations.get("card:list:1")).thenReturn("10");
        when(hashOperations.get("card:positions:10", "1")).thenReturn(null);

        Integer result = cacheService.getCardPosition(1L);

        assertNull(result);
    }

    // ====================================================================
    // GET CARDS BY LIST
    // ====================================================================

    @Test
    @DisplayName("GetCardsByList - returns map of card positions on cache hit")
    void getCardsByList_WhenCached_ShouldReturnPositionMap() {
        Map<Object, Object> redisData = new HashMap<>();
        redisData.put("1", 0);
        redisData.put("2", 1);
        redisData.put("3", 2);

        when(hashOperations.entries("card:positions:10")).thenReturn(redisData);

        Map<Long, Integer> result = cacheService.getCardsByList(10L);

        assertThat(result).hasSize(3);
        assertEquals(0, result.get(1L));
        assertEquals(1, result.get(2L));
        assertEquals(2, result.get(3L));
    }

    @Test
    @DisplayName("GetCardsByList - returns empty map on cache miss")
    void getCardsByList_WhenNotCached_ShouldReturnEmptyMap() {
        when(hashOperations.entries("card:positions:10")).thenReturn(new HashMap<>());

        Map<Long, Integer> result = cacheService.getCardsByList(10L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("GetCardsByList - returns empty map when Redis throws exception")
    void getCardsByList_WhenRedisFails_ShouldReturnEmptyMap() {
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("Redis down"));

        Map<Long, Integer> result = cacheService.getCardsByList(10L);

        assertThat(result).isEmpty();
    }

    // ====================================================================
    // INVALIDATE CACHE
    // ====================================================================

    @Test
    @DisplayName("InvalidateCache - removes card from Hash and deletes list-mapping key")
    void invalidateCache_ShouldRemoveCardFromHashAndDeleteMapping() {
        // card:list:1 → "10"
        when(valueOperations.get("card:list:1")).thenReturn("10");
        when(hashOperations.delete(anyString(), any(Object[].class))).thenReturn(1L);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> cacheService.invalidateCache(1L));

        // Should have removed the card from the list's position Hash
        verify(hashOperations).delete("card:positions:10", "1");

        // Should have deleted the card->list mapping key
        verify(redisTemplate).delete("card:list:1");
    }

    @Test
    @DisplayName("InvalidateCache - does nothing when list mapping key missing")
    void invalidateCache_WhenNoListMapping_ShouldOnlyDeleteMappingKey() {
        when(valueOperations.get("card:list:99")).thenReturn(null);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> cacheService.invalidateCache(99L));

        // Hash delete should NOT be called since we don't know which list
        verify(hashOperations, never()).delete(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("InvalidateCache - does NOT throw when Redis is unavailable")
    void invalidateCache_WhenRedisFails_ShouldNotThrow() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        assertDoesNotThrow(() -> cacheService.invalidateCache(1L));
    }

    // ====================================================================
    // CLEAR LIST CACHE
    // ====================================================================

    @Test
    @DisplayName("ClearListCache - deletes the whole position Hash for a list")
    void clearListCache_ShouldDeletePositionHash() {
        when(redisTemplate.delete("card:positions:10")).thenReturn(true);

        assertDoesNotThrow(() -> cacheService.clearListCache(10L));

        verify(redisTemplate).delete("card:positions:10");
    }

    @Test
    @DisplayName("ClearListCache - does NOT throw when Redis is unavailable")
    void clearListCache_WhenRedisFails_ShouldNotThrow() {
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertDoesNotThrow(() -> cacheService.clearListCache(10L));
    }

    // ====================================================================
    // WARM LIST CACHE
    // ====================================================================

    @Test
    @DisplayName("WarmListCache - bulk stores all card positions in Redis")
    void warmListCache_ShouldBulkStorePositions() {
        Map<Long, Integer> positions = new HashMap<>();
        positions.put(1L, 0);
        positions.put(2L, 1);
        positions.put(3L, 2);

        doNothing().when(hashOperations).putAll(anyString(), anyMap());
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertDoesNotThrow(() -> cacheService.warmListCache(10L, positions));

        // Should have called putAll on the hash once (bulk insert)
        verify(hashOperations).putAll(eq("card:positions:10"), anyMap());

        // TTL should be set
        verify(redisTemplate).expire(eq("card:positions:10"), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("WarmListCache - does nothing when positions map is empty")
    void warmListCache_WhenEmpty_ShouldDoNothing() {
        assertDoesNotThrow(() -> cacheService.warmListCache(10L, new HashMap<>()));

        // Should not touch Redis at all
        verify(hashOperations, never()).putAll(anyString(), anyMap());
    }

    @Test
    @DisplayName("WarmListCache - does nothing when positions map is null")
    void warmListCache_WhenNull_ShouldDoNothing() {
        assertDoesNotThrow(() -> cacheService.warmListCache(10L, null));

        verify(hashOperations, never()).putAll(anyString(), anyMap());
    }
}
