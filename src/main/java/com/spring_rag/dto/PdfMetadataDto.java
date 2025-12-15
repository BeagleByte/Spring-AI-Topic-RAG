package com.spring_rag.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java. util.Map;

public class PdfMetadataDto {
    private String fileName;
    private String contentType;
    private long fileSize;
    private String title;
    private String author;
    private String subject;
    private String creator;
    private String producer;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;
    private int pageCount;
    private String keywords;
    private String extractedText;
    private String publishingYear;
    private Map<String, String> allMetadata;

    // Constructors
    public PdfMetadataDto() {
        this.allMetadata = new HashMap<>();
    }

    // Utility method to check if metadata key exists
    public boolean containsKey(String key) {
        return allMetadata != null && allMetadata.containsKey(key);
    }

    // Utility method to get metadata value
    public String get(String key) {
        return allMetadata != null ? allMetadata.get(key) : null;
    }

    // Utility method to put metadata value
    public void put(String key, String value) {
        if (allMetadata == null) {
            allMetadata = new HashMap<>();
        }
        allMetadata.put(key, value);
    }

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getPublishingYear() {
        return publishingYear;
    }

    public void setPublishingYear(String publishingYear) {
        this.publishingYear = publishingYear;
    }

    public Map<String, String> getAllMetadata() {
        return allMetadata;
    }

    public void setAllMetadata(Map<String, String> allMetadata) {
        this.allMetadata = allMetadata;
    }
}