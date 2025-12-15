package com.spring_rag.service;

import com.spring_rag.exception.TopicNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import io.qdrant.client.QdrantClient;
import com.spring_rag.config.TopicConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for managing Vector Stores for different topics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreFactory {

    private final QdrantClient qdrantClient;
    private final TopicConfig topicConfig;
    private final EmbeddingModel embeddingModel;
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    /**
     * Get vector store for a topic (creates if not exists)
     */
    public VectorStore getVectorStore(String topic) {
        if (!topicConfig.hasTopic(topic)) {
            throw new TopicNotFoundException("Unknown topic: " + topic);
        }

        if (vectorStoreCache.containsKey(topic)) {
            log.debug("Using cached vector store for topic: {}", topic);
            return vectorStoreCache.get(topic);
        }

        String collectionName = topicConfig.getCollectionName(topic);

        try {
            // Use Builder pattern to create QdrantVectorStore
            QdrantVectorStore vectorStore = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                    .collectionName(collectionName)
                    .build();

            vectorStoreCache.put(topic, vectorStore);
            log.info("Created vector store for topic '{}' with collection '{}'", topic, collectionName);
            return vectorStore;
        } catch (Exception ex) {
            log.error("Failed to create vector store for topic: {}", topic, ex);
            throw new RuntimeException("Failed to create vector store: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get all topic stats
     */
    public Map<String, Object> getCollectionStats() {
        Map<String, Object> stats = new HashMap<>();

        topicConfig.getAllTopics().forEach((topic, info) -> {
            try {
                String collectionName = info.getCollectionName();
                Map<String, Object> topicStats = new HashMap<>();
                topicStats.put("collection", collectionName);
                topicStats.put("description", info.getDescription());
                topicStats.put("status", "active");

                // Get vector count from Qdrant collection
                try {
                    var collectionInfo = qdrantClient.getCollectionInfoAsync(collectionName).get();
                    long vectorCount = collectionInfo.getPointsCount();
                    topicStats.put("vectors_count", vectorCount);
                    log.debug("Collection '{}' has {} vectors", collectionName, vectorCount);
                } catch (Exception ex) {
                    log.warn("Failed to get vector count for collection '{}': {}", collectionName, ex.getMessage());
                    topicStats.put("vectors_count", 0);
                }

                stats.put(topic, topicStats);
            } catch (Exception ex) {
                log.warn("Error getting stats for topic: {}", topic, ex);
                stats.put(topic, Map.of("error", ex.getMessage()));
            }
        });

        return stats;
    }
}