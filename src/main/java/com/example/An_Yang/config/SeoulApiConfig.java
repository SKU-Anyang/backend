package com.example.An_Yang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SeoulApiConfig {
    @Bean(name = "seoulClient")
    public WebClient seoulClient(@Value("${apis.seoul.baseUrl}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}