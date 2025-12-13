package com.spring_rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.  document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org. springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org. springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.spring_rag.dto.DocumentResponse;
import com.spring_rag.model.DocumentMetadata;
import com.spring_rag.config.TopicConfig;

import java.nio.charset.StandardCharsets;
import java.time. Instant;
import java.util.*;
import java.util.concurrent. atomic.AtomicInteger;

/**
 * Document service that routes to topic-specific RAGs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicDocumentService {

    private final VectorStoreFactory vectorStoreFactory;
    private final DocumentMetadataStore metadataStore;
    private final PdfMetadataExtractor pdfMetadataExtractor;
    private final TopicConfig topicConfig;

    /**
     * Upload PDF to a specific topic RAG
     */
    public Mono<DocumentResponse> uploadPdfToTopic(String topic, FilePart filePart) {
        // Validate topic
        if (!topicConfig.hasTopic(topic)) {
            return Mono.error(new IllegalArgumentException("Unknown topic: " + topic));
        }

        log.info("Uploading PDF to topic RAG: {}", topic);

        return filePart.  content()
                .collectList()
                .map(buffers -> {
                    int totalSize = buffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
                    byte[] pdfBytes = new byte[totalSize];
                    int pos = 0;
                    for (var buffer : buffers) {
                        int readable = buffer.readableByteCount();
                        buffer.read(pdfBytes, pos, readable);
                        pos += readable;
                    }
                    return pdfBytes;
                })
                .map(pdfBytes -> {
                    String docId = UUID.randomUUID().toString();
                    String filename = filePart.filename();
                    long uploadedAt = System.currentTimeMillis();

                    // Extract PDF metadata
                    Map<String, Object> pdfMetadata = pdfMetadataExtractor.extractMetadata(pdfBytes);
                    log.info("Extracted PDF metadata for topic '{}': {}",
                            topic, pdfMetadataExtractor.prettyPrintMetadata(pdfMetadata));

                    // Read PDF
                    ByteArrayResource resource = new ByteArrayResource(pdfBytes);
                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                    List<Document> documents = pdfReader.get();

                    // Split into chunks
                    TokenTextSplitter splitter = new TokenTextSplitter();
                    List<Document> splitDocuments = splitter.split(documents);

                    // Enrich metadata with topic information
                    AtomicInteger chunkIndex = new AtomicInteger(0);
                    splitDocuments.forEach(doc -> {
                        Map<String, Object> metadata = doc.getMetadata();

                        // Basic info
                        metadata.put("docId", docId);
                        metadata.put("filename", filename);
                        metadata.put("documentType", "pdf");
                        metadata.put("topic", topic);  // IMPORTANT: topic identifier
                        metadata.put("chunkIndex", chunkIndex.getAndIncrement());
                        metadata.put("uploadedAt", uploadedAt);
                        metadata.put("uploadedAtISO", Instant.ofEpochMilli(uploadedAt).toString());

                        // PDF metadata
                        if (pdfMetadata.containsKey("title")) {
                            metadata.put("title", pdfMetadata.get("title"));
                        }
                        if (pdfMetadata.containsKey("author")) {
                            metadata.put("author", pdfMetadata.get("author"));
                        }
                        if (pdfMetadata.containsKey("publishingYear")) {
                            metadata.put("publishingYear", pdfMetadata.get("publishingYear"));
                        }
                    });

                    // Get topic-specific vector store and store documents
                    VectorStore topicVectorStore = vectorStoreFactory. getVectorStore(topic);
                    topicVectorStore.add(splitDocuments);

                    log.info("Stored {} chunks in {} topic RAG", splitDocuments.size(), topic);

                    // Create response
                    String title = (String) pdfMetadata.getOrDefault("title", filename);
                    String author = (String) pdfMetadata.getOrDefault("author", "Unknown");
                    Integer publishingYear = (Integer) pdfMetadata.getOrDefault("publishingYear", null);

                    DocumentResponse response = DocumentResponse.builder()
                            . id(docId)
                            . filename(filename)
                            .title(title)
                            .author(author)
                            .publishingYear(publishingYear)
                            .type("pdf")
                            .topic(topic)  // Include topic in response
                            .chunksCount(splitDocuments.size())
                            .status("indexed")
                            .uploadedAt(uploadedAt)
                            .build();

                    // Cache metadata
                    metadataStore.store(docId, DocumentMetadata.builder()
                            .id(docId)
                            . filename(filename)
                            . title(title)
                            .author(author)
                            .publishingYear(publishingYear)
                            .type("pdf")
                            .topic(topic)
                            .chunksCount(splitDocuments.size())
                            .uploadedAt(uploadedAt)
                            .build());

                    return response;
                })
                .onErrorResume(ex -> {
                    log.error("Error uploading PDF to topic:  {}", topic, ex);
                    return Mono.error(new RuntimeException("Failed to upload PDF to topic " + topic + ": " + ex.getMessage()));
                });
    }

    /**
     * Upload Markdown to a specific topic RAG
     */
    public Mono<DocumentResponse> uploadMarkdownToTopic(String topic, FilePart filePart) {
        if (!topicConfig.hasTopic(topic)) {
            return Mono.error(new IllegalArgumentException("Unknown topic: " + topic));
        }

        log.info("Uploading Markdown to topic RAG: {}", topic);

        return filePart. content()
                .collectList()
                .map(buffers -> {
                    int totalSize = buffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
                    byte[] mdBytes = new byte[totalSize];
                    int pos = 0;
                    for (var buffer : buffers) {
                        int readable = buffer.readableByteCount();
                        buffer.read(mdBytes, pos, readable);
                        pos += readable;
                    }
                    return new String(mdBytes, StandardCharsets. UTF_8);
                })
                .map(content -> {
                    String docId = UUID.randomUUID().toString();
                    String filename = filePart.filename();
                    long uploadedAt = System.currentTimeMillis();

                    // Split by sections
                    List<Document> documents = new ArrayList<>();
                    String[] sections = content.split("(?m)^---\\s*$");

                    for (int i = 0; i < sections.length; i++) {
                        String section = sections[i]. trim();
                        if (! section.isEmpty()) {
                            Document doc = new Document(section);
                            doc.getMetadata().put("section", i);
                            documents. add(doc);
                        }
                    }

                    // Split further
                    TokenTextSplitter splitter = new TokenTextSplitter();
                    List<Document> splitDocuments = splitter.split(documents);

                    // Enrich metadata with topic
                    AtomicInteger chunkIndex = new AtomicInteger(0);
                    splitDocuments.forEach(doc -> {
                        Map<String, Object> metadata = doc.getMetadata();
                        metadata.put("docId", docId);
                        metadata.put("filename", filename);
                        metadata.put("documentType", "markdown");
                        metadata.put("topic", topic);  // Topic identifier
                        metadata.put("chunkIndex", chunkIndex. getAndIncrement());
                        metadata.put("uploadedAt", uploadedAt);
                        metadata.put("uploadedAtISO", Instant.ofEpochMilli(uploadedAt).toString());
                    });

                    // Store in topic-specific vector store
                    VectorStore topicVectorStore = vectorStoreFactory.getVectorStore(topic);
                    topicVectorStore.add(splitDocuments);

                    log.info("Stored {} chunks in {} topic RAG", splitDocuments.size(), topic);

                    DocumentResponse response = DocumentResponse.builder()
                            .id(docId)
                            .filename(filename)
                            .title(filename)
                            .type("markdown")
                            .topic(topic)
                            .chunksCount(splitDocuments.size())
                            .status("indexed")
                            .uploadedAt(uploadedAt)
                            .build();

                    metadataStore.store(docId, DocumentMetadata.builder()
                            .id(docId)
                            .filename(filename)
                            .title(filename)
                            .type("markdown")
                            .topic(topic)
                            .chunksCount(splitDocuments.size())
                            .uploadedAt(uploadedAt)
                            .build());

                    return response;
                })
                .onErrorResume(ex -> {
                    log.error("Error uploading Markdown to topic: {}", topic, ex);
                    return Mono.error(new RuntimeException("Failed to upload Markdown to topic " + topic + ": " + ex.getMessage()));
                });
    }
}