# Spring AI RAG Multi-Topic System

A production-ready multi-topic Retrieval-Augmented Generation (RAG) system built with Spring AI, Ollama, and Qdrant.

## Features

✨ **Multi-Topic RAGs**:  Separate isolated RAGs for different domains (Pentesting, IoT, Blockchain, Cloud, etc.)  
🔍 **Intelligent Retrieval**: Semantic search using vector embeddings  
📄 **Document Support**: PDF and Markdown files with automatic metadata extraction  
🤖 **Local LLM**: Runs entirely locally using Ollama  
🔒 **No External APIs**: All processing happens on your machine  
⚡ **Fast Indexing**: Efficient vector storage with Qdrant  
🚀 **Easy to Extend**: Add new topics with simple configuration

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Ollama installed locally
- Maven

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/beaglebyte/spring-ai-rag-multi-topic.git
   cd spring-ai-rag-multi-topic