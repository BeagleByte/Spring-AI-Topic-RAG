# Spring AI RAG Multi-Topic System

## Overview

Your **Spring-AI-Topic-RAG** project is a production-ready **Retrieval-Augmented Generation (RAG) system** built with Spring AI, Ollama, and Qdrant. Tthis project demonstrates an advanced implementation of AI-powered document processing and semantic search.

## What You Built

### Core Features:

- **Multi-Topic RAG System**: Separate, isolated RAG instances for different domains (Pentesting, IoT, Blockchain, Cloud, etc.)
- **Semantic Search**: Intelligent document retrieval using vector embeddings
- **Document Processing**: Support for PDF and Markdown files with automatic metadata extraction
- **Local LLM Processing**: Runs entirely locally using Ollama (no external API dependencies)
- **Vector Storage**: Efficient semantic search using Qdrant
- **Easy Extensibility**: Simple configuration to add new topics

### Technology Stack:

- **Framework**: Spring Boot 3.5.8 with Spring AI 1.1.2
- **Language**: Java 21
- **LLM**: Ollama (local language model)
- **Vector Database**: Qdrant
- **Document Processing**: Apache Tika & PDFBox
- **Communication**: gRPC for Qdrant client
- **Build Tool**: Maven

---

## Step-by-Step Explanation for Beginners

### **What is RAG (Retrieval-Augmented Generation)?**

RAG is a technique that combines:

1. **Retrieval**: Finding relevant documents from a knowledge base
2. **Augmentation**: Using those documents to enhance the AI's response
3. **Generation**: Creating an answer based on both the user's question and the retrieved documents

Think of it as giving an AI assistant a library of books before asking questions!

### **How This Project Works - 5 Steps**

#### **Step 1: Document Ingestion**

Code

```
Your PDF/Markdown Files
        ↓
    Tika & PDFBox (read files)
        ↓
    Extract Text & Metadata
```

- The system reads your documents and extracts text content
- Metadata (author, creation date, etc.) is automatically captured
- Documents are split into manageable chunks

#### **Step 2: Vector Embedding**

Code

```
Document Text
     ↓
Ollama (local LLM)
     ↓
Convert to Vectors (numerical representation)
```

- Each document chunk is converted into a vector (a list of numbers)
- These vectors capture the semantic meaning of the text
- Similar documents have similar vectors

#### **Step 3: Vector Storage (Indexing)**

Code

```
Document Vectors
     ↓
Qdrant Vector Database
     ↓
Organized & Searchable Index
```

- Vectors are stored in Qdrant for fast retrieval
- Organized by topic to keep domains separate
- Enables quick semantic search

#### **Step 4: Query Processing**

Code

```
User Question
     ↓
Convert to Vector (same way as documents)
     ↓
Search Qdrant for Similar Vectors
     ↓
Retrieve Top Matching Documents
```

- When a user asks a question, it's converted to a vector
- The system finds documents with similar vectors
- Only relevant documents are retrieved

#### **Step 5: Response Generation**

Code

```
User Question + Retrieved Documents
     ↓
Ollama (local LLM)
     ↓
Generate Intelligent Answer
```

- The LLM reads the retrieved documents
- It generates an answer grounded in those specific documents
- Response is more accurate and verifiable

### **Multi-Topic Architecture**

Instead of one large database, you have **separate RAG systems for different domains**:

Code

```
Spring AI Application
    ├── Pentesting RAG ──→ Pentesting Documents → Pentesting Vector Store
    ├── IoT RAG ──────────→ IoT Documents → IoT Vector Store
```

**Benefits:**

- Better semantic relevance (avoids mixing unrelated domains)
- Faster searches (smaller databases)
- Easy to manage (add/remove topics independently)

### **Getting Started - Prerequisites**

Before running this project, you need:

|Requirement|Purpose|
|---|---|
|**Java 17+**|Run the Spring application|
|**Maven**|Build & manage dependencies|
|**Docker & Docker Compose**|Run containerized services|
|**Ollama**|Local language model (runs AI locally)|
|**Qdrant**|Vector database (stores & searches vectors)|

### **Key Technologies Explained**

|Technology|Role|
|---|---|
|**Spring Boot**|Web framework for the application|
|**Spring AI**|Abstraction layer for AI/ML operations|
|**Ollama**|Runs large language models locally (privacy-first)|
|**Qdrant**|Specialized database for vector similarity search|
|**Apache Tika**|Extracts text from various document formats|
|**PDFBox**|Reads PDF files and metadata|
|**gRPC**|Fast communication protocol between services|

### **Workflow Example**

**Scenario**: You have PDFs about cloud security

1. **Upload PDFs** → System reads and chunks them
2. **Index** → Each chunk gets converted to vectors and stored in Qdrant
3. **User Asks** → "What are cloud security best practices?"
4. **Search** → Finds relevant chunks about cloud security
5. **Generate** → Ollama writes an answer based on those chunks
6. **Return** → User gets an accurate, sourced answer

---

## Why This Project is Useful

✅ **Private**: Everything runs locally, no data sent to external APIs  
✅ **Accurate**: Answers are grounded in your actual documents  
✅ **Customizable**: Add any topic/domain you need  
✅ **Scalable**: Separate topics mean independent scaling  
✅ **Modern**: Uses cutting-edge Spring AI framework