package com.example.An_Yang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MoisApiConfig {
    @Bean(name = "moisClient")
    public WebClient moisClient(@Value("${apis.mois.baseUrl}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}