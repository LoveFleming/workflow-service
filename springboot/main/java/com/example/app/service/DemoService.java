package com.example.app.service;

import com.example.app.cache.GuavaCacheService;
import com.example.app.processflow.BusinessProcessFlow;
import org.springframework.stereotype.Service;

@Service
public class DemoService {
    private final BusinessProcessFlow processFlow;
    private final GuavaCacheService cacheService;

    public DemoService(BusinessProcessFlow processFlow, GuavaCacheService cacheService) {
        this.processFlow = processFlow;
        this.cacheService = cacheService;
    }

    public String process(String input) {
        String cacheKey = "business:" + input;
        String cached = (String) cacheService.get(cacheKey);
        if (cached != null) return cached;
        String result = processFlow.execute(input);
        cacheService.put(cacheKey, result);
        return result;
    }
}