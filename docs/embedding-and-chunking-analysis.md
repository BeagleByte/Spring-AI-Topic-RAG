# 📊 Embedding Model & PDF Chunking Analysis

## 🔍 Where is the EmbeddingModel Used?

### 1. **VectorStoreFactory.java** (Line 26)
```java
@RequiredArgsConstructor
public class VectorStoreFactory {
    private final QdrantClient qdrantClient;
    private final TopicConfig topicConfig;
    private final EmbeddingModel embeddingModel;  // ← Injected by Spring
}
```

**Purpose:** The `EmbeddingModel` is injected into the factory and used to create vector stores.

---

### 2. **Creating QdrantVectorStore** (VectorStoreFactory.java, Line 46-48)
```java
QdrantVectorStore vectorStore = QdrantVectorStore.builder(qdrantClient, embeddingModel)
        .collectionName(collectionName)
        .build();
```

**What happens here:**
- The `embeddingModel` is passed to `QdrantVectorStore.builder()`
- This embedding model is used **internally** by Spring AI to convert text into vectors

---

### 3. **When Embeddings are Generated** (Automatic)

#### A) **During Document Upload** (TopicDocumentService.java, Line 107)
```java
VectorStore topicVectorStore = vectorStoreFactory.getVectorStore(topic);
topicVectorStore.add(splitDocuments);  // ← Embeddings generated HERE!
```

**Process:**
1. You upload a PDF
2. PDF gets chunked (see below)
3. `vectorStore.add()` is called
4. **Spring AI automatically calls the EmbeddingModel** to convert each text chunk into a vector
5. Vectors are stored in Qdrant with metadata

#### B) **During Search Queries** (TopicRagService.java, Line 53)
```java
List<Document> relevantDocs = topicVectorStore.similaritySearch(searchRequest);
```

**Process:**
1. User sends a query: "What is penetration testing?"
2. **Spring AI automatically calls the EmbeddingModel** to convert the query into a vector
3. Qdrant searches for similar vectors (cosine similarity)
4. Returns the most relevant document chunks

---

## ✂️ Where PDFs Get Chunked

### Location: **TopicDocumentService.java**

#### **Step 1: Read PDF** (Lines 73-75)
```java
ByteArrayResource resource = new ByteArrayResource(pdfBytes);
PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
List<Document> documents = pdfReader.get();  // ← Reads PDF page by page
```
- Uses **Spring AI's PagePdfDocumentReader**
- Extracts text from each PDF page
- Creates one `Document` object per page

---

#### **Step 2: Split into Chunks** (Lines 76-77)
```java
TokenTextSplitter splitter = new TokenTextSplitter();
List<Document> splitDocuments = splitter.split(documents);  // ← CHUNKING HAPPENS HERE!
```

**What TokenTextSplitter does:**
- Takes the page-level documents
- Splits them into **smaller chunks** based on token count
- Default configuration (you can customize):
  - **Chunk size:** ~800 tokens (~3000 characters)
  - **Chunk overlap:** ~400 tokens (prevents losing context at boundaries)

**Example:**
```
Original PDF (3 pages):
├── Page 1: 2000 tokens
├── Page 2: 1800 tokens  
└── Page 3: 1500 tokens

After TokenTextSplitter:
├── Chunk 0: 800 tokens (from Page 1, start)
├── Chunk 1: 800 tokens (from Page 1, middle) [400 token overlap with Chunk 0]
├── Chunk 2: 800 tokens (from Page 1 end + Page 2 start)
├── Chunk 3: 800 tokens (from Page 2)
├── Chunk 4: 800 tokens (from Page 2 end + Page 3 start)
└── Chunk 5: 700 tokens (from Page 3, end)

Total: 6 chunks with overlaps for context preservation
```

---

#### **Step 3: Enrich Metadata** (Lines 79-103)
```java
splitDocuments.forEach(doc -> {
    Map<String, Object> metadata = doc.getMetadata();
    metadata.put("docId", docId);
    metadata.put("filename", filename);
    metadata.put("topic", topic);  // ← Topic for routing
    metadata.put("chunkIndex", chunkIndex.getAndIncrement());
    metadata.put("title", pdfMetadata.get("title"));
    metadata.put("author", pdfMetadata.get("author"));
    metadata.put("publishingYear", pdfMetadata.get("publishingYear"));
});
```

Each chunk now has:
- Document ID
- Original filename
- Topic (pentesting, iot, etc.)
- Chunk index (for ordering)
- PDF metadata (title, author, year)

---

#### **Step 4: Convert to Vectors & Store** (Lines 106-107)
```java
VectorStore topicVectorStore = vectorStoreFactory.getVectorStore(topic);
topicVectorStore.add(splitDocuments);  // ← Embeddings created & stored in Qdrant
```

**Behind the scenes:**
```
For each chunk:
1. Text: "Penetration testing involves..."
2. EmbeddingModel.embed(text) → [0.234, -0.123, 0.456, ..., 0.789] (768 dimensions)
3. Store in Qdrant:
   - Vector: [0.234, -0.123, ...]
   - Metadata: {filename: "pentest.pdf", topic: "pentesting", chunkIndex: 0}
```

---

## 🔄 Complete Flow Diagram

```
PDF Upload (pentest.pdf, 50 pages)
    ↓
[1] PagePdfDocumentReader.get()
    → Extract text from 50 pages
    → Create 50 Document objects
    ↓
[2] TokenTextSplitter.split()
    → Split 50 pages into ~200 chunks (depending on content)
    → Each chunk: ~800 tokens with 400 token overlap
    ↓
[3] Enrich with metadata
    → Add docId, filename, topic, chunkIndex, author, year
    ↓
[4] vectorStore.add(splitDocuments)
    → For each of 200 chunks:
        a) EmbeddingModel converts text → vector (768 dimensions)
        b) Store vector + metadata in Qdrant collection
    ↓
[5] Indexed! Ready for RAG queries
```

---

## 🎯 RAG Query Flow

```
User Query: "What are the phases of penetration testing?"
    ↓
[1] EmbeddingModel.embed(query)
    → Convert query to vector: [0.123, -0.456, ...]
    ↓
[2] Qdrant similarity search in "pentesting" collection
    → Find top 5 most similar vectors (cosine similarity)
    → Return corresponding text chunks + metadata
    ↓
[3] Build context from retrieved chunks
    → Combine chunk texts with metadata
    ↓
[4] Send to Ollama LLM
    → Prompt: "Answer based on this context: [chunks]... Question: [query]"
    ↓
[5] LLM generates answer
    → Uses retrieved chunks as knowledge base
    → Returns synthesized answer
```

---

## 📦 Key Components Summary

| Component | Purpose | Location |
|-----------|---------|----------|
| **EmbeddingModel** | Converts text ↔ vectors | Injected by Spring AI |
| **PagePdfDocumentReader** | Extracts text from PDF | TopicDocumentService.java:73-75 |
| **TokenTextSplitter** | Chunks text into smaller pieces | TopicDocumentService.java:76-77 |
| **QdrantVectorStore** | Stores vectors + metadata | VectorStoreFactory.java:46-48 |
| **VectorStore.add()** | Triggers embedding generation | TopicDocumentService.java:107 |
| **VectorStore.similaritySearch()** | Queries with embeddings | TopicRagService.java:53 |

---

## 🔧 Where is the Embedding Model Configured?

### **Answer: It's Auto-Configured by Spring AI!** ✨

You don't explicitly configure the `EmbeddingModel` bean - Spring Boot does it automatically via **auto-configuration**.

### **How It Works:**

#### 1. **Maven Dependency** (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```
This starter includes:
- `OllamaEmbeddingModel` implementation
- `OllamaChatModel` implementation
- Auto-configuration classes

#### 2. **Configuration in application.yaml**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434  # ← Ollama server URL
      model: llama2                      # ← Default model for chat
```

**Important:** The embedding model in Ollama uses **the same base-url** but typically uses a **different model** optimized for embeddings.

#### 3. **Spring AI Auto-Configuration**
When your application starts:
```
Spring Boot detects spring-ai-starter-model-ollama
    ↓
Auto-configures OllamaEmbeddingModel bean
    ↓
Uses configuration from spring.ai.ollama.base-url
    ↓
By default, uses model: "nomic-embed-text" or "mxbai-embed-large"
    ↓
Bean is injected into VectorStoreFactory
```

#### 4. **Which Embedding Model is Actually Used?**

Spring AI's Ollama starter uses **Ollama's default embedding model**, typically:
- **`nomic-embed-text`** (most common, 768 dimensions)
- **`mxbai-embed-large`** (alternative, 1024 dimensions)
- **`all-minilm`** (smaller, 384 dimensions)

To check which model Ollama is using:
```bash
# List available embedding models
ollama list

# Pull a specific embedding model if needed
ollama pull nomic-embed-text
```

### **How to Change the Embedding Model?**

#### Option 1: **Add explicit configuration in application.yaml** (Recommended)
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama2          # For chat/text generation
      embedding:
        model: nomic-embed-text  # For embeddings (explicit)
```

#### Option 2: **Create a custom @Bean** (Advanced)
Create a new config file:
```java
@Configuration
public class EmbeddingConfig {
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .withBaseUrl("http://localhost:11434")
                .withModel("nomic-embed-text")  // Specify model
                .build();
    }
}
```

### **Current Setup in Your Project:**

✅ **Dependency:** `spring-ai-starter-model-ollama` in pom.xml  
✅ **Base URL:** `http://localhost:11434` in application.yaml  
✅ **Auto-configured:** `EmbeddingModel` bean created automatically  
✅ **Injected:** Into `VectorStoreFactory` via `@RequiredArgsConstructor`  
⚠️ **Embedding Model:** Uses Ollama's default (likely `nomic-embed-text`)  

### **To Verify What's Being Used:**

Check your application logs when it starts:
```
DEBUG org.springframework.ai.ollama - Using Ollama embedding model: nomic-embed-text
```

Or make a test API call to see the model in action!

---

## 💡 Key Takeaways

1. **You never directly call the embedding model** - Spring AI handles it internally
2. **Embeddings are generated twice:**
   - When storing documents (`vectorStore.add()`)
   - When searching (`similaritySearch()`)
3. **Chunking happens in TopicDocumentService** using `TokenTextSplitter`
4. **Each chunk becomes a vector** stored in Qdrant
5. **Metadata travels with vectors** for context in answers

This is a clean, well-architected RAG system! 🚀

