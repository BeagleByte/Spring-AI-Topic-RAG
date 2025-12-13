package com.spring_rag.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant client configuration
 */
@Slf4j
@Configuration
public class QdrantConfig {
    
    @Value("${spring.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;
    
    @Value("${spring.vectorstore.qdrant.port:6334}")
    private int qdrantPort;
    
    @Bean
    public QdrantClient qdrantClient() {
        log.info("Initializing Qdrant client:  {}:{}", qdrantHost, qdrantPort);

        // Create QdrantClient directly with host and port (Qdrant Java client API)
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
        );
    }
}