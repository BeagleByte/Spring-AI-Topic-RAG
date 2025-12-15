package com.spring_rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceReference {
    private String filename;
    private String title;
    private String author;
    private String publishingYear;
    private String type;
}