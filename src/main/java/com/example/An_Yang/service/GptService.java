package com.example.An_Yang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GptService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;

    @Value("${gemini.api-key:${GOOGLE_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.max-output-tokens:400}") // 숫자만 뽑으므로 줄임
    private Integer maxOutputTokens;

    @Value("${gemini.temperature:0.2}") // 숫자 안정화 위해 낮춤
    private Double temperature;

    private final ObjectMapper om = new ObjectMapper();

    /* ---------- 기본 chat ---------- */
    public String chat(String prompt) { return chatMono(prompt).block(); }

    public String chat(String system, String user) {
        String merged = "[SYSTEM]\n" + system + "\n\n[USER]\n" + user;
        return chat(merged);
    }

    public Mono<String> chatMono(String prompt) {
        var body = new GenerateContentRequest(
                List.of(new Content(List.of(new Part(prompt)))),
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

    /* ---------- JSON 스키마 강제 호출(숫자/배열 추출용) ---------- */
    public JsonNode generateJsonWithSchema(String prompt, Map<String, Object> responseSchema) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", Map.of(
                "maxOutputTokens", maxOutputTokens,
                "temperature",     temperature,
                "response_mime_type", "application/json",
                "response_schema", responseSchema
        ));

        // 429 완화: 최대 3회 지연 재시도
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            try {
                JsonNode res = geminiWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/models/{model}:generateContent")
                                .queryParam("key", apiKey)
                                .build(model))
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
                if (res == null) return null;
                String text = res.at("/candidates/0/content/parts/0/text").asText(null);
                if (text == null) return null;
                return om.readTree(text);
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    try { Thread.sleep(350L * attempts); } catch (InterruptedException ignored) {}
                    continue;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** 숫자 하나만 필요할 때(소수점 1자리 권고) */
    public Double askNumberOnly(String instruction) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("value", Map.of("type","number")));
        schema.put("required", List.of("value"));

        String prompt = """
                아래 지시를 따르고 결과는 JSON으로만 응답하라.
                {"value": <숫자>}
                지시: %s
                """.formatted(instruction);

        JsonNode json = generateJsonWithSchema(prompt, schema);
        if (json != null && json.has("value")) {
            try { return json.get("value").asDouble(); } catch (Exception ignored) {}
        }
        // JSON 실패 시 텍스트 숫자 파싱 백업
        String txt = chat(instruction + "\n\n숫자만 한 줄로 답해.");
        return firstDouble(txt);
    }

    /** 여러 연도 숫자 배열 요청: [{"year":YYYY,"rate":n.n}, ...] */
    public Map<Integer, Double> askYearRates(String industry, String region, int startYear, int endYear) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "object");
        item.put("properties", Map.of(
                "year", Map.of("type","integer"),
                "rate", Map.of("type","number")
        ));
        item.put("required", List.of("year", "rate"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("values", Map.of("type","array", "items", item)));
        schema.put("required", List.of("values"));

        String prompt = """
                한국 소상공인 업종 '%s'(지역 힌트: %s)에 대해 %d~%d년 폐업률(%%)을 추정하라.
                - 숫자만 필요
                - 연도별 1개 값
                - 응답은 JSON {"values":[{"year":YYYY,"rate":n.n},...]} 형식으로만.
                - 데이터가 불확실하면 한국 평균/유사 카테고리 통계를 근거로 보수적으로 추정.
                """.formatted(industry, region, startYear, endYear);

        JsonNode json = generateJsonWithSchema(prompt, schema);
        Map<Integer, Double> out = new LinkedHashMap<>();
        if (json != null && json.has("values") && json.get("values").isArray()) {
            for (JsonNode n : json.get("values")) {
                try {
                    int y = n.get("year").asInt();
                    double r = n.get("rate").asDouble();
                    out.put(y, r);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    private Double firstDouble(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)").matcher(s);
        if (m.find()) {
            try { return Double.parseDouble(m.group()); } catch (Exception ignored) {}
        }
        return null;
    }

    /* ---------- BusinessRecommendService 시그니처(기존) ---------- */
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
                - 각 아이디어: 타깃·핵심가치·제품/서비스·초기비용 범위·예상 리스크·차별화 포인트 (한 줄씩)
                - 과장 금지, 한국 소상공인 맥락
                """.formatted(
                nullToDash(type), nullToDash(location),
                budget == null ? "-" : budget.toString(),
                nullToDash(riskLevel), nullToDash(extra)
        );
        return chat(prompt);
    }
    private String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    /* ---------- 비즈니스 설계(JSON 스키마) 기존 ---------- */
    public AiPlan generatePlan(Map<String, Object> ctx) {
        String prompt = """
                너는 한국 소상공인 창업 컨설턴트다.
                아래 입력과 참고지표를 반영해
                1) 입지 전략, 2) 타깃 분석, 3) 콘셉트 차별화, 4) 수익 전략을 작성하고
                5) 가게명 5개를 제안하며,
                6) 주요 리스크(why, severity 1~5, mitigation)를 3~5개 생성하라.
                한국어로 간결히. 수치가 모호하면 '데이터 부족'이라 명시.

                [입력]
                업종=%s, 지역=%s, 타깃=%s, 예산=%,d원, 인력=%d명, 면적=%.1f평, 메모=%s
                [참고지표] 폐업률=%.2f%%
                """.formatted(
                ctx.get("industry"),
                ctx.get("region"),
                ctx.getOrDefault("target",""),
                ((Number)ctx.getOrDefault("budgetKrw", 0)).longValue(),
                ((Number)ctx.getOrDefault("staff", 0)).intValue(),
                ((Number)ctx.getOrDefault("areaPyeong", 0.0)).doubleValue(),
                ctx.getOrDefault("notes",""),
                ((Number)ctx.getOrDefault("closureRate", 0.0)).doubleValue()
        );

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("positioning",     Map.of("type","string"));
        props.put("targeting",       Map.of("type","string"));
        props.put("differentiation", Map.of("type","string"));
        props.put("revenue",         Map.of("type","string"));
        props.put("nameIdeas",       Map.of("type","array", "items", Map.of("type","string")));
        props.put("majorRisks",      Map.of(
                "type","array",
                "items", Map.of(
                        "type","object",
                        "properties", Map.of(
                                "title",      Map.of("type","string"),
                                "why",        Map.of("type","string"),
                                "severity",   Map.of("type","integer"),
                                "mitigation", Map.of("type","string")
                        ),
                        "required", List.of("title","why","severity","mitigation")
                )
        ));

        Map<String, Object> responseSchema = new LinkedHashMap<>();
        responseSchema.put("type", "object");
        responseSchema.put("properties", props);
        responseSchema.put("required",
                List.of("positioning","targeting","differentiation","revenue","nameIdeas","majorRisks"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", Map.of(
                "maxOutputTokens",   maxOutputTokens,
                "temperature",       temperature,
                "response_mime_type","application/json",
                "response_schema",   responseSchema
        ));

        try {
            JsonNode res = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String json = (res == null) ? "{}" : res.at("/candidates/0/content/parts/0/text").asText("{}");
            return om.readValue(json, AiPlan.class);
        } catch (Exception e) {
            AiPlan fallback = new AiPlan();
            fallback.setPositioning("생성 실패");
            fallback.setTargeting("데이터 부족");
            fallback.setDifferentiation("데이터 부족");
            fallback.setRevenue("데이터 부족");
            fallback.setNameIdeas(List.of());
            fallback.setMajorRisks(List.of());
            return fallback;
        }
    }

    /* ---------- DTOs ---------- */
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

    /* ---------- 비즈 설계 응답 ---------- */
    @Getter @Setter
    public static class AiPlan {
        private String positioning;
        private String targeting;
        private String differentiation;
        private String revenue;
        private List<String> nameIdeas;
        private List<Risk> majorRisks;

        @Getter @Setter
        public static class Risk {
            private String title;
            private String why;
            private int severity;
            private String mitigation;
        }
    }
}
