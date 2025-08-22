package com.example.An_Yang.service;

import com.example.An_Yang.entity.ChatHistory;
import com.example.An_Yang.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private final ChatHistoryRepository chatHistoryRepository;

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    private final WebClient webClient;

    public GptService(ChatHistoryRepository chatHistoryRepository,
                      @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                      @Value("${openai.api-key}") String apiKey,
                      @Value("${openai.model}") String model,
                      @Value("${openai.max-tokens:512}") int maxTokens,
                      @Value("${openai.temperature:0.7}") double temperature) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;

        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                                .build()
                )
                .build();
    }

    /** 일반 질문 → GPT 답변 후 히스토리 저장 */
    public String getGptResponse(String question) {
        String systemPrompt = "You are a helpful assistant. Keep answers concise but accurate.";
        String answer = callChatCompletions(systemPrompt, question);

        chatHistoryRepository.save(new ChatHistory(question, answer, LocalDateTime.now()));
        return answer;
    }

    /** 사업 추천 → GPT 답변 후 히스토리 저장 */
    public String recommendBusiness(String businessType, String preferredLocation, Integer budget) {
        String systemPrompt = "You are a seasoned small-business consultant. Give practical, step-by-step and localized suggestions for Korea.";
        String userPrompt = String.format(
                "업종: %s\n선호 지역: %s\n예산(만원): %s\n조건에 맞는 창업 아이디어와 이유, 초기 비용 내역 가안, 리스크 3가지와 완화책을 간단히 알려줘.",
                emptyTo(businessType, "미정"),
                emptyTo(preferredLocation, "미정"),
                budget == null ? "미정" : budget.toString()
        );

        String answer = callChatCompletions(systemPrompt, userPrompt);
        chatHistoryRepository.save(new ChatHistory("[BusinessRecommend]\n" + userPrompt, answer, LocalDateTime.now()));
        return answer;
    }

    /** ----- 내부 공용 메서드 ----- */
    private String callChatCompletions(String system, String user) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", system == null ? "You are a helpful assistant." : system),
                        Map.of("role", "user", "content", user)
                ),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", false
        );

        Map<String, Object> res = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e ->
                        Mono.error(new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e))
                )
                .block();

        if (res == null || !res.containsKey("choices")) {
            throw new RuntimeException("OpenAI 응답이 비어 있습니다.");
        }
        List<?> choices = (List<?>) res.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("OpenAI 응답 choices가 비어 있습니다.");
        }
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        Object content = message.get("content");
        if (content == null) {
            throw new RuntimeException("OpenAI 응답에 content가 없습니다.");
        }
        return content.toString();
    }

    private String emptyTo(String s, String alt) {
        return (s == null || s.isBlank()) ? alt : s;
    }
}
