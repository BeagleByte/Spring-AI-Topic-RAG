package com.spring_rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.spring_rag.config.TopicConfig;
import com.spring_rag.dto.QueryRequest;
import com.spring_rag.dto.QueryResponse;
import com.spring_rag.dto.SourceReference;

import java.util.*;

/**
 * RAG service for topic-specific queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicRagService {

    private final VectorStoreFactory vectorStoreFactory;
    private final ChatModel chatModel;
    private final TopicConfig topicConfig;

    /**
     * Query a specific topic RAG
     */
    public Mono<QueryResponse> queryTopic(String topic, QueryRequest request) {
        // Validate topic
        if (!topicConfig.hasTopic(topic)) {
            return Mono.error(new IllegalArgumentException("Unknown topic: " + topic));
        }

        return Mono.fromCallable(() -> {
                    log.info("RAG Query for topic '{}': {}", topic, request.getQuery());

                    // Get topic-specific vector store
                    VectorStore topicVectorStore = vectorStoreFactory.getVectorStore(topic);

                    // Search in topic collection
                    SearchRequest searchRequest = SearchRequest.builder()
                            .query(request.getQuery())
                            .topK(request.getTopK() > 0 ? request.getTopK() : 5)
                            .build();

                    List<Document> relevantDocs = topicVectorStore.similaritySearch(searchRequest);
                    log.info("Found {} relevant documents in {} topic", relevantDocs.size(), topic);

                    // Build context
                    StringBuilder contextBuilder = new StringBuilder();
                    List<SourceReference> sources = new ArrayList<>();

                    for (Document doc : relevantDocs) {
                        Map<String, Object> metadata = doc.getMetadata();
                        String filename = (String) metadata.getOrDefault("filename", "unknown");
                        String title = (String) metadata.getOrDefault("title", filename);
                        String author = (String) metadata.getOrDefault("author", "Unknown");
                        Integer publishingYear = (Integer) metadata.getOrDefault("publishingYear", null);

                        contextBuilder.append("--- Source: ").append(filename);
                        if (!title.equals(filename)) {
                            contextBuilder.append(" (Title: ").append(title).append(")");
                        }
                        if (!"Unknown".equals(author)) {
                            contextBuilder.append(" | Author: ").append(author);
                        }
                        if (publishingYear != null) {
                            contextBuilder.append(" | Year: ").append(publishingYear);
                        }
                        contextBuilder.append(" ---\n")
                                .append(doc.getText())
                                .append("\n\n");

                        if (sources.stream().noneMatch(s -> s.getFilename().equals(filename))) {
                            sources.add(SourceReference.builder()
                                    .filename(filename)
                                    .title(title)
                                    .author(author)
                                    .publishingYear(publishingYear)
                                    .type((String) metadata.getOrDefault("documentType", "unknown"))
                                    .build());
                        }
                    }

                    String context = contextBuilder.toString();
                    String topicDescription = topicConfig.getAllTopics().get(topic).getDescription();

                    // Create topic-aware prompt
                    String ragPromptTemplate = """
                You are an expert assistant specializing in:  {topicDescription}
                
                Answer questions about {topic} based ONLY on the provided documents.
                
                CONTEXT FROM DOCUMENTS:
                {context}
                
                USER QUESTION:  {query}
                
                INSTRUCTIONS:
                - Answer based ONLY on the provided context
                - If the answer is not in the context, say so
                - When citing information, include the document title, author, and year if available
                - Be concise and clear
                - Focus on {topic}-specific insights
                """;

                    PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);

                    Map<String, Object> vars = new HashMap<>();
                    vars.put("topicDescription", topicDescription);
                    vars.put("topic", topic);
                    vars.put("context", context);
                    vars.put("query", request.getQuery());

                    Prompt prompt = promptTemplate.create(vars);

                    String answer = chatModel.call(prompt).getResult().getOutput().toString();

                    return QueryResponse.builder()
                            .query(request.getQuery())
                            .topic(topic)  // Include topic in response
                            .answer(answer)
                            .sourceCount(relevantDocs.size())
                            .sources(sources)
                            .build();
                })
                .doOnError(ex -> log.error("RAG query failed for topic: {}", topic, ex))
                .onErrorResume(ex -> Mono.error(new RuntimeException("RAG query failed for topic " + topic + ": " + ex.getMessage())));
    }

    /**
     * Cross-topic query (query multiple RAGs)
     */
    public Mono<QueryResponse> queryCrossTopic(List<String> topics, QueryRequest request) {
        return Mono.fromCallable(() -> {
                    log.info("Cross-topic RAG Query across {}:  {}", topics, request.getQuery());

                    List<Document> allRelevantDocs = new ArrayList<>();

                    // Query each topic
                    for (String topic : topics) {
                        if (!topicConfig.hasTopic(topic)) {
                            log.warn("Topic not found: {}, skipping", topic);
                            continue;
                        }

                        VectorStore topicVectorStore = vectorStoreFactory.getVectorStore(topic);
                        SearchRequest searchRequest = SearchRequest.builder()
                                .query(request.getQuery())
                                .topK(3)  // Fewer per topic for cross-topic
                                .build();

                        List<Document> topicDocs = topicVectorStore.similaritySearch(searchRequest);
                        allRelevantDocs.addAll(topicDocs);
                        log.info("Found {} documents in topic:  {}", topicDocs.size(), topic);
                    }

                    // Build context from all topics
                    StringBuilder contextBuilder = new StringBuilder();
                    for (Document doc : allRelevantDocs) {
                        Map<String, Object> metadata = doc.getMetadata();
                        String docTopic = (String) metadata.getOrDefault("topic", "unknown");
                        String filename = (String) metadata.getOrDefault("filename", "unknown");

                        contextBuilder.append("[").append(docTopic.toUpperCase()).append("] ")
                                .append(filename).append(":\n")
                                .append(doc.getText())
                                .append("\n\n");
                    }

                    String context = contextBuilder.toString();

                    // Cross-topic prompt
                    String prompt = """
                You are an expert assistant with knowledge across multiple domains:  {topics}
                
                Answer the following question by synthesizing insights from multiple knowledge domains.
                
                CONTEXT:
                {context}
                
                QUESTION:  {query}
                
                Synthesize the answer across topics and explain how different domains relate to the question.
                """;

                    PromptTemplate promptTemplate = new PromptTemplate(prompt);

                    Map<String, Object> vars = new HashMap<>();
                    vars.put("topics", String.join(", ", topics));
                    vars.put("context", context);
                    vars.put("query", request.getQuery());

                    Prompt finalPrompt = promptTemplate.create(vars);

                    String answer = chatModel.call(finalPrompt).getResult().getOutput().toString();

                    return QueryResponse.builder()
                            .query(request.getQuery())
                            .topics(topics)  // Multiple topics
                            .answer(answer)
                            .sourceCount(allRelevantDocs.size())
                            .build();
                })
                .onErrorResume(ex -> Mono.error(new RuntimeException("Cross-topic query failed: " + ex.getMessage())));
    }
}