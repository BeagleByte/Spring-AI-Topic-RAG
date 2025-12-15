package com.spring_rag.service;

import com.spring_rag.dto.PdfMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.spring_rag.dto.DocumentResponse;
import com.spring_rag.model.DocumentMetadata;
import com.spring_rag.config.TopicConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Document service that routes to topic-specific RAGs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicDocumentService {

    private final VectorStoreFactory vectorStoreFactory;
    private final DocumentMetadataStore metadataStore;
    private final PdfMetadataService pdfMetadataExtractor;
    private final TopicConfig topicConfig;

    /**
     * Upload PDF to a specific topic RAG
     */
    public DocumentResponse uploadPdfToTopic(String topic, MultipartFile file) {
        // Validate topic
        if (!topicConfig.hasTopic(topic)) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }

        log.info("Uploading PDF to topic RAG: {}", topic);

        try {

            String docId = UUID.randomUUID().toString();
            String filename = file.getOriginalFilename();
            long uploadedAt = System.currentTimeMillis();

            // Extract PDF metadata
            PdfMetadataDto pdfMetadata = pdfMetadataExtractor.extractMetadata(file);
            log.info("Extracted PDF metadata for topic '{}': {}",
                    topic, pdfMetadataExtractor.extractMetadata(file));

            // Read PDF
            byte[] pdfBytes = file.getBytes();
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
                if (pdfMetadata.getTitle() != null) {
                    metadata.put("title", pdfMetadata.getTitle());
                }
                if (pdfMetadata.getAuthor()!=null) {
                    metadata.put("author", pdfMetadata.getAuthor());
                }
                if (pdfMetadata.containsKey("publishingYear")) {
                    metadata.put("publishingYear", pdfMetadata.get("publishingYear"));
                }
            });

            // Get topic-specific vector store and store documents
            VectorStore topicVectorStore = vectorStoreFactory.getVectorStore(topic);
            topicVectorStore.add(splitDocuments);

            log.info("Stored {} chunks in {} topic RAG", splitDocuments.size(), topic);

            // Create response
            String title = (String) pdfMetadata.get("title");
            String author = (String) pdfMetadata.get("author");
            Integer publishingYear =  Integer.parseInt(pdfMetadata.get("publishingYear"));

            DocumentResponse response = DocumentResponse.builder()
                    .id(docId)
                    .filename(filename)
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
                    .filename(filename)
                    .title(title)
                    .author(author)
                    .publishingYear(publishingYear)
                    .type("pdf")
                    .topic(topic)
                    .chunksCount(splitDocuments.size())
                    .uploadedAt(uploadedAt)
                    .build());

            return response;
        } catch (IOException ex) {
            log.error("Error uploading PDF to topic: {}", topic, ex);
            throw new RuntimeException("Failed to upload PDF to topic " + topic + ": " + ex.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Upload Markdown to a specific topic RAG
     */
    public DocumentResponse uploadMarkdownToTopic(String topic, MultipartFile file) {
        if (!topicConfig.hasTopic(topic)) {
            throw new IllegalArgumentException("Unknown topic: " + topic);
        }

        log.info("Uploading Markdown to topic RAG: {}", topic);

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String docId = UUID.randomUUID().toString();
            String filename = file.getOriginalFilename();
            long uploadedAt = System.currentTimeMillis();

            // Split by sections
            List<Document> documents = new ArrayList<>();
            String[] sections = content.split("(?m)^---\\s*$");

            for (int i = 0; i < sections.length; i++) {
                String section = sections[i].trim();
                if (!section.isEmpty()) {
                    Document doc = new Document(section);
                    doc.getMetadata().put("section", i);
                    documents.add(doc);
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
                metadata.put("chunkIndex", chunkIndex.getAndIncrement());
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
        } catch (IOException ex) {
            log.error("Error uploading Markdown to topic: {}", topic, ex);
            throw new RuntimeException("Failed to upload Markdown to topic " + topic + ": " + ex.getMessage());
        }
    }
}