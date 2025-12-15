package com.spring_rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.ollama.embedding.model}")
    private String embeddingModelName;

    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        log.info("Configuring OllamaEmbeddingModel with model: {}", embeddingModelName);

        // Use the auto-configured OllamaApi bean (which has the correct base URL)
        OllamaEmbeddingOptions defaultOptions = OllamaEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();

        // Use the OllamaEmbeddingModel builder
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(defaultOptions)
                .build();
    }

}