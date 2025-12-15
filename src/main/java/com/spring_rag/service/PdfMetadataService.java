package com.spring_rag.service;

import com.spring_rag.dto.PdfMetadataDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract PDF metadata including title, author, creation date, producer
 * Uses both Apache Tika and PDFBox for comprehensive metadata extraction
 */
@Slf4j
@Service
public class PdfMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(PdfMetadataService.class);
    private final Tika tika;

    public PdfMetadataService() {
        this.tika = new Tika();
    }

    /**
     * Extract metadata from uploaded PDF file
     */
    /**
     * Extract metadata from uploaded PDF file
     */
    public PdfMetadataDto extractMetadata(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file type
        String contentType = tika.detect(file.getInputStream());
        if (!contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("File is not a PDF.  Detected type: " + contentType);
        }

        PdfMetadataDto metadataDto = new PdfMetadataDto();
        metadataDto.setFileName(file.getOriginalFilename());
        metadataDto.setContentType(contentType);
        metadataDto.setFileSize(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = new Metadata();
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 for unlimited text
            ParseContext context = new ParseContext();

            // Parse the document
            parser.parse(inputStream, handler, metadata, context);

            // Extract standard metadata
            metadataDto.setTitle(metadata.get(TikaCoreProperties.TITLE));
            metadataDto.setAuthor(metadata.get(TikaCoreProperties.CREATOR));
            metadataDto.setSubject(metadata.get(TikaCoreProperties.SUBJECT));
            metadataDto.setCreator(metadata.get("creator"));
            metadataDto.setProducer(metadata.get("producer"));

            // Extract keywords using multiple alternatives
            String keywords = extractKeywords(metadata);
            metadataDto.setKeywords(keywords);

            // Extract dates
            Date created = metadata.getDate(TikaCoreProperties.CREATED);
            if (created != null) {
                metadataDto.setCreationDate(
                        LocalDateTime.ofInstant(created.toInstant(), ZoneId.systemDefault())
                );
            }

            Date modified = metadata.getDate(TikaCoreProperties.MODIFIED);
            if (modified != null) {
                metadataDto.setModificationDate(
                        LocalDateTime.ofInstant(modified.toInstant(), ZoneId.systemDefault())
                );
            }

            // Extract page count
            String pageCountStr = metadata.get("xmpTPg: NPages");
            if (pageCountStr != null) {
                try {
                    metadataDto.setPageCount(Integer.parseInt(pageCountStr));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse page count: {}", pageCountStr);
                }
            }

            // Extract text content (limited to first 10000 characters for performance)
            String extractedText = handler.toString();
            if (extractedText.length() > 10000) {
                extractedText = extractedText.substring(0, 10000) + "...";
            }
            metadataDto.setExtractedText(extractedText);

            // Get all metadata as map
            Map<String, String> allMetadata = new HashMap<>();
            for (String name : metadata.names()) {
                allMetadata.put(name, metadata.get(name));
            }
            metadataDto.setAllMetadata(allMetadata);

            // Extract publishing year from various sources
            String publishingYear = extractPublishingYear(metadata, created);
            if (publishingYear != null) {
                metadataDto.setPublishingYear(publishingYear);
                metadataDto.put("publishingYear", publishingYear);
            }

            logger.info("Successfully extracted metadata from PDF: {}", file.getOriginalFilename());

        } catch (Exception e) {
            logger.error("Error extracting metadata from PDF: {}", file.getOriginalFilename(), e);
            throw new Exception("Failed to extract PDF metadata:  " + e.getMessage(), e);
        }

        return metadataDto;
    }

    /**
     * Extract keywords from metadata using multiple alternatives
     */
    private String extractKeywords(Metadata metadata) {
        // Try multiple possible keyword fields in order of preference
        String[] keywordFields = {
                "Keywords",                           // PDF standard field
                "keywords",                           // lowercase variant
                "pdf: docinfo: keywords",              // PDF-specific
                "meta:keyword",                       // Meta keyword
                "dc:subject",                         // Dublin Core subject
                "subject",                            // Generic subject
                "cp:keywords",                        // Core Properties keywords
                "extended-properties:Keywords"        // Extended properties
        };

        for (String field : keywordFields) {
            String value = metadata.get(field);
            if (value != null && !value.trim().isEmpty()) {
                logger.debug("Found keywords in field '{}': {}", field, value);
                return value;
            }
        }

        // Alternative: Try using DublinCore.SUBJECT if available
        try {
            String dcSubject = metadata.get(DublinCore.SUBJECT);
            if (dcSubject != null && !dcSubject.trim().isEmpty()) {
                logger.debug("Found keywords in DublinCore.SUBJECT: {}", dcSubject);
                return dcSubject;
            }
        } catch (Exception e) {
            logger.debug("DublinCore.SUBJECT not available", e);
        }

        // Alternative: Try using Office. KEYWORDS if available
        try {
            String officeKeywords = metadata.get(Office.KEYWORDS);
            if (officeKeywords != null && !officeKeywords.trim().isEmpty()) {
                logger.debug("Found keywords in Office.KEYWORDS: {}", officeKeywords);
                return officeKeywords;
            }
        } catch (Exception e) {
            logger.debug("Office. KEYWORDS not available", e);
        }

        // Log all available metadata fields for debugging
        logger.debug("Available metadata fields:");
        for (String name : metadata.names()) {
            if (name.toLowerCase().contains("keyword") || name.toLowerCase().contains("subject")) {
                logger.debug("  {} = {}", name, metadata.get(name));
            }
        }

        return null;
    }

    /**
     * Extract publishing year from metadata
     */
    private String extractPublishingYear(Metadata metadata, Date creationDate) {
        // Try to get year from various metadata fields
        String[] possibleYearFields = {
                "pdf:docinfo:custom:PublishingYear",
                "pdf:docinfo:custom:Year",
                "dc:date",
                "dcterms:created",
                "meta:creation-date",
                "Creation-Date",
                "date"
        };

        for (String field : possibleYearFields) {
            String value = metadata.get(field);
            if (value != null) {
                String year = extractYearFromString(value);
                if (year != null) {
                    return year;
                }
            }
        }

        // If no explicit year found, extract from creation date
        if (creationDate != null) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(
                    creationDate.toInstant(),
                    ZoneId.systemDefault()
            );
            return String.valueOf(localDateTime.getYear());
        }

        return null;
    }

    /**
     * Extract year from a string using regex
     */
    private String extractYearFromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Match 4-digit year (1900-2099)
        Pattern yearPattern = Pattern.compile("(19|20)\\d{2}");
        Matcher matcher = yearPattern.matcher(value);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Extract only text content without metadata
     */
    public String extractText(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.parseToString(inputStream);
        } catch (Exception e) {
            logger.error("Error extracting text from PDF: {}", file.getOriginalFilename(), e);
            throw new Exception("Failed to extract PDF text: " + e.getMessage(), e);
        }
    }

    /**
     * Detect content type of uploaded file
     */
    public String detectContentType(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        }
    }
}