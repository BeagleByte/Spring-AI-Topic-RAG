package com.spring_rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadata {
    private String id;
    private String filename;
    private String title;
    private String author;
    private String publishingYear;
    private String type;
    private String topic;         // NEW: topic identifier
    private int chunksCount;
    private long uploadedAt;
}