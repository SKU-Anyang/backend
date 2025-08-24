package com.example.An_Yang.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GptService {

    private final WebClient geminiWebClient;

    @Value("${gemini.api-key:${GOOGLE_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.max-output-tokens:800}")
    private Integer maxOutputTokens;

    @Value("${gemini.temperature:0.4}")
    private Double temperature;

    /* ---------- 기본 chat ---------- */
    public String chat(String prompt) {
        return chatMono(prompt).block();
    }

    public String chat(String system, String user) {
        String merged = "[SYSTEM]\n" + system + "\n\n[USER]\n" + user;
        return chat(merged);
    }

    public Mono<String> chatMono(String prompt) {
        var body = new GenerateContentRequest(
                List.of(new Content(List.of(new Part(prompt)))) ,
                new GenerationConfig(maxOutputTokens, temperature)
        );

        return geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenerateContentResponse.class)
                .map(GenerateContentResponse::firstTextOrEmpty)
                .onErrorResume(e -> Mono.just("[Gemini 호출 실패] " + e.getMessage()));
    }

    /* ---------- AiFacade가 찾는 별칭들 ---------- */
    public String complete(String p)         { return chat(p); }
    public String generate(String p)         { return chat(p); }
    public String ask(String p)              { return chat(p); }
    public String create(String p)           { return chat(p); }
    public String createChat(String p)       { return chat(p); }
    public String createCompletion(String p) { return chat(p); }

    /* ---------- BusinessRecommendService가 호출하는 메서드 (시그니처 맞춤) ---------- */
    // getBusinessRecommendation(String,String,Integer,String,String)
    public String getBusinessRecommendation(String type, String location,
                                            Integer budget, String riskLevel, String extra) {
        String prompt = """
                [ROLE] 창업 추천 컨설턴트
                [INPUT]
                - 업종/타입: %s
                - 위치: %s
                - 예상 예산(만원): %s
                - 리스크 선호도: %s
                - 추가 요구사항: %s

                [TASK]
                - 실행 가능한 창업 아이디어 3개 제시
                - 각 아이디어에 대해 타깃, 핵심가치, 제품/서비스, 초기비용 범위, 예상 리스크, 차별화 포인트를 한 줄씩
                - 과장은 금지하고 한국 소상공인 맥락에 맞게
                """.formatted(
                nullToDash(type), nullToDash(location),
                budget == null ? "-" : budget.toString(),
                nullToDash(riskLevel), nullToDash(extra)
        );
        return chat(prompt);
    }

    private String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    /* ---------- DTOs for REST ---------- */
    record GenerateContentRequest(List<Content> contents, GenerationConfig generationConfig) {}
    record GenerationConfig(Integer maxOutputTokens, Double temperature) {}
    record Content(List<Part> parts) {}
    record Part(String text) {}
    record GenerateContentResponse(List<Candidate> candidates) {
        String firstTextOrEmpty() {
            if (candidates == null || candidates.isEmpty()) return "";
            var c = candidates.get(0);
            if (c == null || c.content == null || c.content.parts == null || c.content.parts.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (var p : c.content.parts) {
                if (p != null && p.text != null) sb.append(p.text);
            }
            return sb.toString().trim();
        }
    }
    record Candidate(Content content) {}
}
