package com.spring_rag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org. springframework.web.bind.annotation. RequestMapping;
import org.springframework. web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.spring_rag.config.TopicConfig;
import com.spring_rag.service.VectorStoreFactory;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final TopicConfig topicConfig;
    private final VectorStoreFactory vectorStoreFactory;

    @GetMapping
    public Mono<Map<String, Object>> health() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = new HashMap<>();
            health. put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("topics_configured", topicConfig.getAllTopics().size());
            health.put("topics", topicConfig.getAllTopics().keySet());

            try {
                Map<String, Object> collectionStats = vectorStoreFactory. getCollectionStats();
                health.put("collections", collectionStats);
                health.put("database_status", "CONNECTED");
            } catch (Exception ex) {
                health.put("database_status", "DISCONNECTED");
                health.put("error", ex.getMessage());
            }

            return health;
        });
    }
}