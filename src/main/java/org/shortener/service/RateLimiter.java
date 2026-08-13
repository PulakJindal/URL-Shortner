package org.shortener.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    int MAX_LIMIT = 100;
    record Entry(int count, Instant createdAt){}
    Cache<String, Entry> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(100_000)
            .build();

    public boolean isInLimit(String ip){
        Instant now = Instant.now();
        Entry entry = cache.getIfPresent(ip);
        if (entry == null || Duration.between(entry.createdAt(), now).toMinutes() >= 1) {
            cache.put(ip, new Entry(1, now));
            return true;
        }
        if (entry.count() >= MAX_LIMIT) {
            return false;
        }
        cache.put(ip, new Entry(entry.count() + 1, entry.createdAt()));
        return true;
    }
}
