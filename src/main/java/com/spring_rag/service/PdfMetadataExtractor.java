package com.spring_rag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Extract PDF metadata including title, author, creation date, producer
 * Uses both Apache Tika and PDFBox for comprehensive metadata extraction
 */
@Slf4j
@Service
public class PdfMetadataExtractor {

    /**
     * Extract all available metadata from PDF bytes
     */
    public Map<String, Object> extractMetadata(byte[] pdfBytes) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Method 1: Use PDFBox for direct PDF document info
            metadata.putAll(extractPdfBoxMetadata(pdfBytes));

            // Method 2: Use Tika for additional fields
            metadata.putAll(extractTikaMetadata(pdfBytes));

        } catch (Exception ex) {
            log.warn("Error extracting PDF metadata, continuing without it", ex);
            // Don't fail the whole process, just skip metadata
        }

        return metadata;
    }

    /**
     * Extract metadata using PDFBox (more direct access to PDF properties)
     */
    private Map<String, Object> extractPdfBoxMetadata(byte[] pdfBytes) {
        Map<String, Object> metadata = new HashMap<>();

        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {

            PDDocumentInformation docInfo = document.getDocumentInformation();

            if (docInfo != null) {
                // Extract title
                if (docInfo.getTitle() != null && !docInfo.getTitle().isEmpty()) {
                    metadata.put("title", docInfo.getTitle());
                    log.debug("Extracted title: {}", docInfo.getTitle());
                }

                // Extract author
                if (docInfo.getAuthor() != null && !docInfo.getAuthor().isEmpty()) {
                    metadata.put("author", docInfo.getAuthor());
                    log.debug("Extracted author: {}", docInfo.getAuthor());
                }

                // Extract creator (application that created the PDF)
                if (docInfo.getCreator() != null && !docInfo.getCreator().isEmpty()) {
                    metadata.put("creator", docInfo.getCreator());
                    log.debug("Extracted creator: {}", docInfo.getCreator());
                }

                // Extract producer (application that last modified it)
                if (docInfo.getProducer() != null && !docInfo.getProducer().isEmpty()) {
                    metadata.put("producer", docInfo.getProducer());
                    log.debug("Extracted producer: {}", docInfo.getProducer());
                }

                // Extract creation date
                Calendar creationDate = docInfo.getCreationDate();
                if (creationDate != null) {
                    long createdTime = creationDate.getTimeInMillis();
                    metadata.put("createdAt", createdTime);

                    // Also store ISO format for readability
                    String isoDate = Instant.ofEpochMilli(createdTime)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    metadata.put("createdAtISO", isoDate);

                    // Extract year
                    int year = creationDate.get(Calendar.YEAR);
                    metadata.put("publishingYear", year);
                    log.debug("Extracted creation date: {} (year: {})", isoDate, year);
                }

                // Extract modification date
                Calendar modDate = docInfo.getModificationDate();
                if (modDate != null) {
                    long modTime = modDate.getTimeInMillis();
                    metadata.put("modifiedAt", modTime);

                    String isoDate = Instant.ofEpochMilli(modTime)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    metadata.put("modifiedAtISO", isoDate);
                    log.debug("Extracted modification date: {}", isoDate);
                }

                // Extract subject
                if (docInfo.getSubject() != null && !docInfo.getSubject().isEmpty()) {
                    metadata.put("subject", docInfo.getSubject());
                    log.debug("Extracted subject: {}", docInfo.getSubject());
                }

                // Extract keywords
                if (docInfo.getKeywords() != null && !docInfo.getKeywords().isEmpty()) {
                    metadata.put("keywords", docInfo.getKeywords());
                    log.debug("Extracted keywords: {}", docInfo.getKeywords());
                }
            }

            // Number of pages
            int pageCount = document.getNumberOfPages();
            metadata.put("pageCount", pageCount);
            log.debug("PDF has {} pages", pageCount);

        } catch (IOException ex) {
            log.warn("Error extracting PDFBox metadata", ex);
        }

        return metadata;
    }

    /**
     * Extract metadata using Apache Tika (catches XMP and other embedded metadata)
     */
    private Map<String, Object> extractTikaMetadata(byte[] pdfBytes) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata tikaMetadata = new Metadata();
            ParseContext context = new ParseContext();

            PDFParser parser = new PDFParser();
            parser.parse(new ByteArrayInputStream(pdfBytes), handler, tikaMetadata, context);

            // Extract all Tika metadata fields
            for (String name : tikaMetadata.names()) {
                String value = tikaMetadata.get(name);

                // Map common Tika fields to our standard names
                switch (name.toLowerCase()) {
                    case "dc:creator", "author" -> {
                        if (!metadata.containsKey("author")) {
                            metadata.put("author", value);
                        }
                    }
                    case "dc: title", "title" -> {
                        if (!metadata.containsKey("title")) {
                            metadata.put("title", value);
                        }
                    }
                    case "pdf:producer" -> {
                        if (!metadata.containsKey("producer")) {
                            metadata.put("producer", value);
                        }
                    }
                    case "xmpmeta:creatortool", "xmp: creatortool" -> {
                        if (!metadata.containsKey("creator")) {
                            metadata.put("creator", value);
                        }
                    }
                    case "dcterms:created" -> metadata.put("tikaCreatedDate", value);
                    case "dcterms:modified" -> metadata.put("tikaModifiedDate", value);
                    case "dc:description" -> metadata.put("description", value);
                    case "dc:subject" -> {
                        if (!metadata.containsKey("subject")) {
                            metadata.put("subject", value);
                        }
                    }
                }

                // Store raw Tika field for reference
                metadata.put("tika_" + name, value);
            }

            log.debug("Extracted {} Tika metadata fields", tikaMetadata.names().length);

        } catch (Exception ex) {
            log.warn("Error extracting Tika metadata", ex);
        }

        return metadata;
    }

    /**
     * Pretty print metadata for logging
     */
    public String prettyPrintMetadata(Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder("PDF Metadata:\n");

        // Standard fields in order
        String[] standardFields = {"title", "author", "creator", "producer",
                "publishingYear", "createdAtISO", "subject", "keywords", "pageCount"};

        for (String field : standardFields) {
            if (metadata.containsKey(field)) {
                sb.append(String.format("  %s: %s\n", field, metadata.get(field)));
            }
        }

        return sb.toString();
    }
}