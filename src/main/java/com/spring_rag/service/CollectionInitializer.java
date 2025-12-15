package com.spring_rag.service;

import com.spring_rag.config.TopicConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Initializes Qdrant collections for all configured topics at application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionInitializer {

    private final QdrantClient qdrantClient;
    private final TopicConfig topicConfig;
    private static final int VECTOR_SIZE = 768; // nomic-embed-text produces 768-dimensional vectors

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCollections() {
        log.info("Initializing Qdrant collections for all topics...");

        topicConfig.getAllTopics().forEach((topic, info) -> {
            String collectionName = info.getCollectionName();
            try {
                // Check if collection exists
                boolean exists = collectionExists(collectionName);

                if (exists) {
                    log.info("✓ Collection '{}' for topic '{}' already exists", collectionName, topic);
                } else {
                    // Create collection
                    createCollection(collectionName);
                    log.info("✓ Created collection '{}' for topic '{}'", collectionName, topic);
                }
            } catch (Exception e) {
                log.error("✗ Failed to initialize collection '{}' for topic '{}': {}",
                        collectionName, topic, e.getMessage(), e);
            }
        });

        log.info("Collection initialization complete!");
    }

    private boolean collectionExists(String collectionName) throws ExecutionException, InterruptedException {
        try {
            qdrantClient.getCollectionInfoAsync(collectionName).get();
            return true;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof io.grpc.StatusRuntimeException) {
                io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) e.getCause();
                if (statusException.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                    return false;
                }
            }
            throw e;
        }
    }

    private void createCollection(String collectionName) throws ExecutionException, InterruptedException {
        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                .setSize(VECTOR_SIZE)
                .setDistance(Collections.Distance.Cosine)
                .build();

        Collections.CreateCollection createCollection = Collections.CreateCollection.newBuilder()
                .setCollectionName(collectionName)
                .setVectorsConfig(Collections.VectorsConfig.newBuilder()
                        .setParams(vectorParams)
                        .build())
                .build();

        qdrantClient.createCollectionAsync(
                collectionName,
                vectorParams
        ).get();
    }
}

