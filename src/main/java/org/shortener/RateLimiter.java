package org.shortener;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    int MAX_LIMIT = 100;
    record Entry(int count, Instant createdAt){}
    Map<String, Entry> cache;

    public RateLimiter(){
        cache = new ConcurrentHashMap<>();
    }

    public boolean isInLimit(String ip){
        Instant now = Instant.now();
        Entry entry = cache.get(ip);
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
