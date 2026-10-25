package com.example.springbootboilerplate.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple single-node Redis lock: SET key token NX PX ttl to acquire,
 * compare-and-delete (Lua) to release so we never remove another holder's lock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * @return the lock token if acquired, empty if someone else holds the lock
     */
    public Optional<String> tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    public void unlock(String key, String token) {
        try {
            Long released = redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
            if (released == null || released == 0) {
                // TTL expired and possibly another request took the lock; nothing of ours to delete.
                log.warn("Lock {} was not held by this token at release time", key);
            }
        } catch (RuntimeException ex) {
            // The work already committed; a failed release only means the lock lives until its TTL.
            log.error("Failed to release lock {}", key, ex);
        }
    }
}
