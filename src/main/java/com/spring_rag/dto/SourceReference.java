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
    private String title;          // NOW INCLUDED
    private String author;         // NOW INCLUDED
    private Integer publishingYear; // NOW INCLUDED
    private String type;
}