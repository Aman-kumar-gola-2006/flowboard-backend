package com.flowboard.card.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CardPositionCacheService - Redis-backed cache for card positions.
 *
 * Why do we need this?
 * When a user drags a card from position 3 to position 0 on the Kanban board,
 * we need to reorder ALL cards in that list. That's a lot of DB writes.
 * Redis lets us update positions in memory first (< 5ms), acknowledge the UI,
 * then flush to MySQL asynchronously.
 *
 * Redis Hash structure:
 *   Key   → "card:positions:{listId}"
 *   Field → "{cardId}"      (String)
 *   Value → position index  (Integer stored as JSON)
 *
 * TTL: 30 minutes. If Redis goes down, the service gracefully falls back to DB.
 */
@Service
public class CardPositionCacheService {

    private static final Logger log = LoggerFactory.getLogger(CardPositionCacheService.class);

    // Key prefix - easy to scan with "redis-cli KEYS card:positions:*"
    private static final String POSITION_KEY_PREFIX = "card:positions:";

    // Also track which list a card belongs to (needed for quick invalidation)
    private static final String CARD_LIST_KEY_PREFIX = "card:list:";

    // 30 min TTL - long enough to cover a user's session, short enough to not go stale
    private static final long CACHE_TTL_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Store a card's position in Redis.
     *
     * Called after every createCard() and moveCard() so the cache stays fresh.
     * We also store the listId mapping so we know which Hash to invalidate later.
     *
     * @param cardId    the card being positioned
     * @param listId    the list it belongs to
     * @param position  its 0-based position index within the list
     */
    public void cacheCardPosition(Long cardId, Long listId, Integer position) {
        try {
            String positionKey = POSITION_KEY_PREFIX + listId;
            String cardListKey = CARD_LIST_KEY_PREFIX + cardId;

            // Store position in the list's Hash: card:positions:{listId} -> {cardId: position}
            redisTemplate.opsForHash().put(positionKey, cardId.toString(), position);

            // Also remember which list this card is in so we can invalidate by cardId alone
            redisTemplate.opsForValue().set(cardListKey, listId.toString(), CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            // Refresh TTL on the position hash too
            redisTemplate.expire(positionKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            log.debug("Cached position {} for card {} in list {}", position, cardId, listId);

        } catch (Exception e) {
            // Redis failure should NEVER crash the card operation
            // Just log it and move on - DB is the source of truth anyway
            log.warn("Redis cache write failed for card {} (non-fatal): {}", cardId, e.getMessage());
        }
    }

    /**
     * Get the cached position of a single card.
     *
     * Quick Redis lookup before hitting the DB. Returns null on cache miss
     * so callers can fall back to MySQL.
     *
     * @param cardId the card to look up
     * @return the cached position, or null if not in cache
     */
    public Integer getCardPosition(Long cardId) {
        try {
            // First find which list this card is in
            Object listIdObj = redisTemplate.opsForValue().get(CARD_LIST_KEY_PREFIX + cardId);
            if (listIdObj == null) {
                log.debug("Cache miss - no list mapping found for card {}", cardId);
                return null;
            }

            String listId = listIdObj.toString();
            String positionKey = POSITION_KEY_PREFIX + listId;

            // Now grab the position from the list's Hash
            Object posObj = redisTemplate.opsForHash().get(positionKey, cardId.toString());
            if (posObj == null) {
                log.debug("Cache miss - no position found for card {} in list {}", cardId, listId);
                return null;
            }

            // Jackson serializes integers as Integer, but Redis might return it as a different type
            return ((Number) posObj).intValue();

        } catch (Exception e) {
            log.warn("Redis cache read failed for card {} (non-fatal): {}", cardId, e.getMessage());
            return null; // caller falls back to DB
        }
    }

    /**
     * Get all cached card positions for a given list.
     *
     * Returns a Map of { cardId (Long) -> position (Integer) }.
     * Used in getCardsByList() to check if we can skip the DB query entirely.
     *
     * @param listId the list to look up
     * @return map of card positions, empty map on cache miss or error
     */
    public Map<Long, Integer> getCardsByList(Long listId) {
        try {
            String positionKey = POSITION_KEY_PREFIX + listId;

            // Grab everything from the Hash in one shot
            Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(positionKey);

            if (rawMap == null || rawMap.isEmpty()) {
                log.debug("Cache miss for list {} - no position data in Redis", listId);
                return Collections.emptyMap();
            }

            // Convert from Map<Object, Object> to Map<Long, Integer>
            Map<Long, Integer> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
                try {
                    Long cardId = Long.parseLong(entry.getKey().toString());
                    Integer position = ((Number) entry.getValue()).intValue();
                    result.put(cardId, position);
                } catch (NumberFormatException ex) {
                    log.warn("Skipping malformed cache entry for list {}: {}", listId, entry.getKey());
                }
            }

            log.debug("Cache HIT for list {} - {} cards found in Redis", listId, result.size());
            return result;

        } catch (Exception e) {
            log.warn("Redis cache read for list {} failed (non-fatal): {}", listId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Remove a single card from the cache.
     *
     * Called on card archive or delete so stale data doesn't linger.
     * Also cleans up the list-mapping key for that card.
     *
     * @param cardId the card to evict
     */
    public void invalidateCache(Long cardId) {
        try {
            // Find the list so we can remove from the right Hash
            Object listIdObj = redisTemplate.opsForValue().get(CARD_LIST_KEY_PREFIX + cardId);
            if (listIdObj != null) {
                String positionKey = POSITION_KEY_PREFIX + listIdObj.toString();
                redisTemplate.opsForHash().delete(positionKey, cardId.toString());
                log.debug("Evicted card {} from position cache (list {})", cardId, listIdObj);
            }

            // Clean up the list-mapping key too
            redisTemplate.delete(CARD_LIST_KEY_PREFIX + cardId);

        } catch (Exception e) {
            log.warn("Redis invalidation failed for card {} (non-fatal): {}", cardId, e.getMessage());
        }
    }

    /**
     * Wipe all cached positions for an entire list.
     *
     * Useful after a bulk reorder operation (moveCard across lists) or
     * when we want to force a fresh load from DB.
     *
     * @param listId the list whose cache should be cleared
     */
    public void clearListCache(Long listId) {
        try {
            String positionKey = POSITION_KEY_PREFIX + listId;
            Long deleted = redisTemplate.delete(positionKey) ? 1L : 0L;
            log.info("Cleared position cache for list {} ({} key deleted)", listId, deleted);
        } catch (Exception e) {
            log.warn("Redis clear for list {} failed (non-fatal): {}", listId, e.getMessage());
        }
    }

    /**
     * Warm the cache for a list from DB-loaded cards.
     *
     * Called when getCardsByList() gets a cache miss and loads from MySQL.
     * We pipe all the positions back into Redis so subsequent requests are fast.
     *
     * @param listId   the list being warmed
     * @param positions map of cardId -> position loaded from DB
     */
    public void warmListCache(Long listId, Map<Long, Integer> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        try {
            String positionKey = POSITION_KEY_PREFIX + listId;

            // Bulk insert using HMSET - one round trip to Redis
            Map<String, Object> redisMap = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : positions.entrySet()) {
                redisMap.put(entry.getKey().toString(), entry.getValue());
                // Also update the card->list mapping key
                redisTemplate.opsForValue().set(
                        CARD_LIST_KEY_PREFIX + entry.getKey(),
                        listId.toString(),
                        CACHE_TTL_MINUTES,
                        TimeUnit.MINUTES
                );
            }
            redisTemplate.opsForHash().putAll(positionKey, redisMap);
            redisTemplate.expire(positionKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            log.info("Warmed Redis cache for list {} with {} card positions", listId, positions.size());

        } catch (Exception e) {
            log.warn("Redis cache warming for list {} failed (non-fatal): {}", listId, e.getMessage());
        }
    }
}
