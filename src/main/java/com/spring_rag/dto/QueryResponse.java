package com.spring_rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryResponse {
    private String query;
    private String topic;         // NEW: single topic
    private List<String> topics;  // NEW: multiple topics (for cross-topic)
    private String answer;
    private int sourceCount;
    private List<SourceReference> sources;
}