package com.flowboard.card.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for the Card Service.
 *
 * We use Redis primarily to cache card positions per list so that
 * drag-drop reorders are fast. The idea: write to Redis first (optimistic),
 * then persist to MySQL in the background. If MySQL fails, we invalidate
 * the Redis key so it's re-read from DB on next request.
 *
 * Key pattern:  card:positions:{listId}  →  Redis Hash { cardId -> position }
 */
@Configuration
public class RedisConfig {

    /**
     * The main RedisTemplate bean we'll inject into CardPositionCacheService.
     *
     * Keys are plain Strings (human-readable, easier to debug with redis-cli).
     * Values are JSON via Jackson so we can store Integer positions without
     * worrying about Java serialization versioning.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use plain String for keys - easier to inspect in redis-cli
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use Jackson JSON for values - handles Integer, Long, etc cleanly
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());          // handles LocalDate / LocalDateTime
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @SuppressWarnings("unchecked")
        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // This is important - without afterPropertiesSet() the template won't initialise properly
        template.afterPropertiesSet();

        return template;
    }
}
