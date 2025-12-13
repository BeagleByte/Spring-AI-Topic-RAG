package com.spring_rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring-rag")
@Data
public class TopicConfig {
    private Map<String, TopicInfo> topics;
    
    @Data
    public static class TopicInfo {
        private String collectionName;
        private String description;
        private String model;
    }
    
    /**
     * Get collection name for a topic
     */
    public String getCollectionName(String topic) {
        TopicInfo info = topics.get(topic);
        if (info == null) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }
        return info.getCollectionName();
    }
    
    /**
     * Check if topic exists
     */
    public boolean hasTopic(String topic) {
        return topics.containsKey(topic);
    }
    
    /**
     * Get all topics
     */
    public Map<String, TopicInfo> getAllTopics() {
        return topics;
    }
}