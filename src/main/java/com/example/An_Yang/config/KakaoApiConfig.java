package com.example.An_Yang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KakaoApiConfig {

    @Bean(name = "kakaoClient")
    public WebClient kakaoClient(
            @Value("${apis.kakao.baseUrl}") String baseUrl,
            @Value("${apis.kakao.restKey}") String restKey
    ) {
        // Kakao 로컬 API: Authorization 헤더 필수
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restKey)
                .build();
    }
}
