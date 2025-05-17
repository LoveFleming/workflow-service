package com.example.app.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class GuavaCacheService {
    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    public void put(String key, Object value) { cache.put(key, value); }
    public Object get(String key) { return cache.getIfPresent(key); }
    public void invalidate(String key) { cache.invalidate(key); }
}