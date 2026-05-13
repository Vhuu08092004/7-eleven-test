package com.seveneleven.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RefreshTokenRepository {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RefreshTokenRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String token, Long userId, long expirationDays) {
        String key = REFRESH_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId, expirationDays, TimeUnit.DAYS);
    }

    public Long findUserIdByToken(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    public void delete(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    public boolean exists(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
