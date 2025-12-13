package com.spring_rag.controller;

import com.spring_rag.config.TopicConfig;
import com.spring_rag.dto.DocumentResponse;
import com.spring_rag.dto.QueryRequest;
import com.spring_rag.dto.QueryResponse;
import com.spring_rag.service.TopicDocumentService;
import com.spring_rag.service.TopicRagService;
import com.spring_rag.service.VectorStoreFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TopicRagController {
    
    private final TopicDocumentService documentService;
    private final TopicRagService ragService;
    private final TopicConfig topicConfig;
    private final VectorStoreFactory vectorStoreFactory;
    
    /**
     * Get all available topics
     */
    @GetMapping
    public Mono<Map<String, TopicConfig. TopicInfo>> getTopics() {
        return Mono.just(topicConfig.getAllTopics());
    }
    
    /**
     * Get collection stats for all topics
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return Mono.fromCallable(vectorStoreFactory::getCollectionStats);
    }
    
    /**
     * Upload PDF to a specific topic
     */
    @PostMapping(value = "/{topic}/documents/upload/pdf",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<DocumentResponse> uploadPdfToTopic(
            @PathVariable String topic,
            @RequestPart("file") FilePart file) {
        log.info("Uploading PDF to topic '{}':  {}", topic, file.filename());
        return documentService.uploadPdfToTopic(topic, file);
    }
    
    /**
     * Upload Markdown to a specific topic
     */
    @PostMapping(value = "/{topic}/documents/upload/markdown",
                 consumes = MediaType. MULTIPART_FORM_DATA_VALUE)
    public Mono<DocumentResponse> uploadMarkdownToTopic(
            @PathVariable String topic,
            @RequestPart("file") FilePart file) {
        log.info("Uploading Markdown to topic '{}': {}", topic, file. filename());
        return documentService. uploadMarkdownToTopic(topic, file);
    }
    
    /**
     * Query a specific topic RAG
     */
    @PostMapping(value = "/{topic}/query",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<QueryResponse> queryTopic(
            @PathVariable String topic,
            @RequestBody QueryRequest request) {
        log.info("Query topic '{}':  {}", topic, request.getQuery());
        return ragService.queryTopic(topic, request);
    }
    
    /**
     * Query across multiple topics (cross-topic search)
     */
    @PostMapping(value = "/query/cross",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<QueryResponse> queryCrossTopic(
            @RequestParam List<String> topics,
            @RequestBody QueryRequest request) {
        log.info("Cross-topic query across {}: {}", topics, request. getQuery());
        return ragService.queryCrossTopic(topics, request);
    }
}