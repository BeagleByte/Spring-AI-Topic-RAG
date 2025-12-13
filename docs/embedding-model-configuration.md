# 🎯 Embedding Model Configuration Guide

## Quick Answer

**The embedding model is AUTO-CONFIGURED by Spring AI!** You don't need to manually create a bean.

---

## Current Configuration

### 1. **Maven Dependency** (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
```

### 2. **Application Configuration** (application.yaml)
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      model: llama2  # This is for chat, NOT embeddings
```

### 3. **Auto-Configuration Magic** ✨
```
Spring Boot Startup
    ↓
Detects: spring-ai-starter-model-ollama
    ↓
Creates: OllamaEmbeddingModel bean (automatically)
    ↓
Configures: base-url from application.yaml
    ↓
Uses Model: nomic-embed-text (Ollama's default)
    ↓
Injects: Into VectorStoreFactory
```

---

## Which Embedding Model is Used?

By default, Spring AI with Ollama uses:
- **Primary:** `nomic-embed-text` (768 dimensions) - Best general-purpose
- **Alternative:** `mxbai-embed-large` (1024 dimensions) - More detailed
- **Lightweight:** `all-minilm` (384 dimensions) - Faster, less accurate

---

## How to Check Your Model

### Method 1: Check Ollama
```bash
# List all models (including embedding models)
ollama list

# Should see something like:
# nomic-embed-text:latest    274MB    2 weeks ago
```

### Method 2: Check Application Logs
When your app starts, look for:
```
DEBUG org.springframework.ai.ollama - Initialized OllamaEmbeddingModel with model: nomic-embed-text
```

### Method 3: Check in Qdrant
```bash
# Check vector dimensions in Qdrant collection
curl http://localhost:6333/collections/spring-rag-pentesting-docs

# Response will show:
# "vector_size": 768  ← This means nomic-embed-text (768 dimensions)
```

---

## How to Change the Embedding Model

### Option 1: Configuration File (Recommended) ⭐

Add to **application.yaml**:
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama2              # For text generation
      embedding:
        model: nomic-embed-text    # Explicitly set embedding model
        # OR
        # model: mxbai-embed-large
        # model: all-minilm
```

### Option 2: Custom Bean (Advanced)

Create **`EmbeddingConfig.java`**:
```java
package com.spring_rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new OllamaEmbeddingModel(
            new OllamaApi(ollamaBaseUrl),
            OllamaEmbeddingModel.OllamaEmbeddingOptions.builder()
                .withModel("nomic-embed-text")  // Specify your model here
                .build()
        );
    }
}
```

---

## Available Embedding Models in Ollama

| Model | Dimensions | Size | Best For |
|-------|-----------|------|----------|
| **nomic-embed-text** | 768 | 274 MB | General purpose (RECOMMENDED) |
| **mxbai-embed-large** | 1024 | 669 MB | High accuracy, slower |
| **all-minilm** | 384 | 45 MB | Speed, lower accuracy |
| **bge-large** | 1024 | 1.34 GB | Multilingual support |

### To Install a Model:
```bash
# Pull the embedding model
ollama pull nomic-embed-text

# Verify it's installed
ollama list | grep embed
```

---

## Important Notes

### ⚠️ **Vector Dimensions Must Match!**

If you change the embedding model, you must:
1. **Delete old Qdrant collections** (different vector dimensions)
2. **Re-upload all documents** (re-generate embeddings)

```bash
# Delete Qdrant collection
curl -X DELETE http://localhost:6333/collections/spring-rag-pentesting-docs
curl -X DELETE http://localhost:6333/collections/spring-rag-iot-docs

# Restart your app (collections will be recreated with new dimensions)
# Re-upload all your PDF documents
```

### ✅ **Best Practice**

**Use `nomic-embed-text`** unless you have a specific reason to change:
- Well-tested
- Good balance of speed/accuracy
- 768 dimensions (standard)
- Widely used in production

---

## Troubleshooting

### Problem: "Model not found"
```
Error: model 'nomic-embed-text' not found
```

**Solution:**
```bash
ollama pull nomic-embed-text
```

### Problem: "Connection refused to localhost:11434"
```
Error: Connection refused
```

**Solution:**
```bash
# Start Ollama
ollama serve

# Or check if it's running
curl http://localhost:11434/api/tags
```

### Problem: Different vector dimensions error
```
Error: Vector dimension mismatch
```

**Solution:** You changed the model. Delete collections and re-upload documents.

---

## Testing Your Embedding Model

Create a simple test endpoint:

```java
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final EmbeddingModel embeddingModel;
    
    @GetMapping("/embedding")
    public Map<String, Object> testEmbedding() {
        String text = "This is a test";
        List<Double> vector = embeddingModel.embed(text);
        
        return Map.of(
            "text", text,
            "dimensions", vector.size(),
            "first_3_values", vector.subList(0, 3)
        );
    }
}
```

Test it:
```bash
curl http://localhost:8080/api/test/embedding

# Response:
{
  "text": "This is a test",
  "dimensions": 768,           ← Confirms nomic-embed-text
  "first_3_values": [0.234, -0.123, 0.456]
}
```

---

## Summary

✅ **Current Setup:** Auto-configured by Spring AI  
✅ **Model Used:** `nomic-embed-text` (768 dimensions)  
✅ **Configuration:** `application.yaml` → `spring.ai.ollama.base-url`  
✅ **Change Model:** Add `spring.ai.ollama.embedding.model` to config  
⚠️ **After Change:** Delete collections and re-upload documents  

Your RAG system is ready to go! 🚀

