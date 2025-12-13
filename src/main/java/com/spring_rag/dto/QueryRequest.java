package com.spring_rag.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String query;
    private int topK = 5;  // Number of relevant documents to retrieve
}
