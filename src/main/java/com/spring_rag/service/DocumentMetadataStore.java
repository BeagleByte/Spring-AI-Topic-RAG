package com.spring_rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework. stereotype.Component;
import com.spring_rag.model.DocumentMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for document metadata
 * For production, use Redis or a database
 */
@Slf4j
@Component
public class DocumentMetadataStore {

    private final Map<String, DocumentMetadata> store = new ConcurrentHashMap<>();

    public void store(String docId, DocumentMetadata metadata) {
        store.put(docId, metadata);
        log.info("Cached metadata for document: {}", docId);
    }

    public DocumentMetadata get(String docId) {
        return store. get(docId);
    }

    public void delete(String docId) {
        store.remove(docId);
    }

    public void clear() {
        store.clear();
    }
}