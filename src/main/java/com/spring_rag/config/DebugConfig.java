package com.spring_rag.config;


import org.springframework.ai.ollama.api.OllamaApi;
import org. springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework. context.annotation.Bean;
import org.springframework.context.annotation. Configuration;

@Configuration
public class DebugConfig {

    @Bean
    public CommandLineRunner debugBeans(ApplicationContext context) {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("ALL OllamaApi BEANS IN CONTEXT:");

            String[] beanNames = context.getBeanNamesForType(OllamaApi.class);
            for (String beanName : beanNames) {
                OllamaApi bean = context. getBean(beanName, OllamaApi.class);
                System.out.println("Bean name: " + beanName);
                System.out.println("Bean class: " + bean.getClass());
                System.out.println("Bean instance: " + bean);
            }

            System.out.println("========================================\n");
        };
    }
}