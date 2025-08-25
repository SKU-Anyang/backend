package com.example.An_Yang.service;

import com.example.An_Yang.DTO.FtcItem;
import com.example.An_Yang.DTO.FtcItems;
import com.example.An_Yang.DTO.FtcResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Year;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FtcNationwideService {

    private final WebClient client;
    private final String serviceKey;
    private final String op;
    private final String resultTypeParam;   // 빈 값이면 붙이지 않음
    private final String resultTypeValue;
    private final XmlMapper xml = new XmlMapper();

    private final GptService gpt; // ★ AI 폴백

    public FtcNationwideService(
            @Qualifier("ftcClient") WebClient client,
            @Value("${apis.ftc.serviceKey}") String serviceKey,
            @Value("${apis.ftc.op}") String op,
            @Value("${apis.ftc.resultTypeParam:}") String resultTypeParam,
            @Value("${apis.ftc.resultTypeValue:}") String resultTypeValue,
            GptService gpt
    ) {
        this.client = client.mutate()
                .defaultHeader("Accept", "application/xml, application/json;q=0.8")
                .build();
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.op = op == null ? "" : op.trim();
        this.resultTypeParam = resultTypeParam == null ? "" : resultTypeParam.trim();
        this.resultTypeValue = resultTypeValue == null ? "" : resultTypeValue.trim();
        this.gpt = gpt;
    }

    /* ========= 원자료 조회 ========= */
    public Mono<List<FtcItem>> getOutfood(int yr, int pageNo, int rows) { return call(yr, "01", pageNo, rows); }
    public Mono<List<FtcItem>> getRetail (int yr, int pageNo, int rows) { return call(yr, "02", pageNo, rows); }
    public Mono<List<FtcItem>> getService(int yr, int pageNo, int rows) { return call(yr, "03", pageNo, rows); }
    public Mono<String> rawOutfood(int yr, int pageNo, int rows) { return rawCall(yr, "01", pageNo, rows); }

    private Mono<List<FtcItem>> call(int yr, String indutyLclsCd, int pageNo, int rows) {
        return rawCall(yr, indutyLclsCd, pageNo, rows)
                .flatMap(body -> Mono.fromCallable(() -> xml.readValue(body, FtcResponse.class)))
                .map(resp -> Optional.ofNullable(resp)
                        .map(FtcResponse::items)
                        .map(FtcItems::item)
                        .orElseGet(List::of))
                .onErrorMap(ex -> new RuntimeException("[FTC] XML 파싱 실패: " + ex.getMessage(), ex));
    }

    private Mono<String> rawCall(int yr, String indutyLclsCd, int pageNo, int rows) {
        int safePage = Math.max(1, pageNo);
        int safeRows  = Math.max(1, rows);

        return client.get()
                .uri(uri -> {
                    var b = uri.path("/" + op)
                            .queryParam("serviceKey", serviceKey)  // 인코딩은 WebClient가 처리
                            .queryParam("yr", yr)
                            .queryParam("indutyLclsCd", indutyLclsCd)
                            .queryParam("pageNo", safePage)
                            .queryParam("numOfRows", safeRows);
                    if (!resultTypeParam.isBlank() && !resultTypeValue.isBlank()) {
                        b.queryParam(resultTypeParam, resultTypeValue);
                    }
                    return b.build();
                })
                .retrieve()
                .onStatus(s -> s.isError(), resp -> resp.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[FTC] " + resp.statusCode() + " - " + truncate(body, 1200))))
                .bodyToMono(String.class);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }

    /* ========= 폐업률 집계(FTC → AI → 기본값) ========= */

    public ClosureRateRecord safeGetClosureRate(String industry, String region) {
        try {
            return getClosureRate(industry, region);
        } catch (Exception e) {
            return new ClosureRateRecord(region, industry, null, null, "FTC");
        }
    }

    /** 최신 확정연도 1개 값 */
    public ClosureRateRecord getClosureRate(String industry, String region) {
        int year = Year.now().getValue() - 1;

        // 1) FTC 시도
        Double rate = computeRateByYear_FTC(industry, year);

        String source = "FTC";
        // 2) FTC 실패 → AI 폴백
        if (rate == null) {
            rate = aiEstimateRate(industry, region, year);
            if (rate != null) source = "FTC/AI";
        }
        // 3) AI도 실패 → 기본값(대분류별 보수 추정)
        if (rate == null) {
            rate = defaultRateByLcls(mapIndustryToCode(industry));
            source = "FTC/AI/DEFAULT";
        }
        return new ClosureRateRecord(region, industry, year, round1(rate), source);
    }

    /** 연도 구간(기본 최근 5년) 시리즈 */
    public List<YearRate> getClosureRateSeries(String industry, String region,
                                               Integer startYear, Integer endYear) {
        int end   = (endYear == null) ? Year.now().getValue() - 1 : endYear;
        int start = (startYear == null) ? end - 4 : startYear;
        if (start > end) { int t = start; start = end; end = t; }

        String lcls = mapIndustryToCode(industry);

        // 1차: FTC 개별 시도
        List<YearRate> out = new ArrayList<>();
        for (int y = start; y <= end; y++) {
            out.add(new YearRate(y, computeRateByYear_FTC(industry, y)));
        }

        // 2차: AI 일괄 폴백(FTC null인 연도만 채움)
        Map<Integer, Double> ai = gpt.askYearRates(industry, region, start, end);
        for (int i = 0; i < out.size(); i++) {
            YearRate yr = out.get(i);
            if (yr.value == null && ai != null) {
                Double v = ai.get(yr.year);
                if (v != null) out.set(i, new YearRate(yr.year, round1(v)));
            }
        }

        // 3차: 기본값 최종 폴백
        for (int i = 0; i < out.size(); i++) {
            YearRate yr = out.get(i);
            if (yr.value == null) {
                out.set(i, new YearRate(yr.year, defaultRateByLcls(lcls)));
            }
        }
        return out;
    }

    /** FTC로 한 해의 폐업률 계산 */
    private Double computeRateByYear_FTC(String industry, int year) {
        String lcls = mapIndustryToCode(industry);

        // A. DTO 파싱
        try {
            List<FtcItem> items = call(year, lcls, 1, 1000).block();
            Double r = rateFromItems(items);
            if (r != null) return r;
        } catch (Exception ignored) {}

        // B. XML 트리 스캔
        try {
            String xmlText = rawCall(year, lcls, 1, 1000).block();
            if (xmlText == null || xmlText.isBlank()) return null;

            // 에러 응답 흔적이면 포기
            String low = xmlText.toLowerCase();
            if (Stream.of("service", "invalid", "error", "limit", "unauthorized", "not", "fail", "is_not_registered")
                    .anyMatch(low::contains)) {
                return null;
            }

            JsonNode root = xml.readTree(xmlText);

            // 1) 비율 필드 우선
            Double rate = firstNumber(root, "closeRate","closureRate","clsRate","clsbizRate","폐업률","close_rt","rate","rt");
            if (rate != null) return rate;

            // 2) 개수로 계산: close / (open+close) * 100
            double close = sumNumbers(root, "clsbizQy","clsbizCnt","closeCnt","close","폐업","폐점","close_biz");
            double open  = sumNumbers(root, "opbizQy","opbizCnt","openCnt","open","개업","신규","open_biz","new_biz");
            if (open + close > 0) return (close / (open + close)) * 100.0;
        } catch (Exception ignored) {}

        return null;
    }

    /** 제미나이 숫자만 추정 */
    private Double aiEstimateRate(String industry, String region, int year) {
        String instruction = """
                한국 소상공인 업종 '%s'(지역 힌트: %s)의 %d년 폐업률(%%)을 보수적으로 추정하라.
                - 숫자만 필요
                - 소수점 1자리 정도
                - 근거 설명 금지
                """.formatted(industry, region, year);
        return gpt.askNumberOnly(instruction);
    }

    /* DTO -> 숫자 합산/추출 */
    @SuppressWarnings("unchecked")
    private Double rateFromItems(List<FtcItem> items) {
        if (items == null || items.isEmpty()) return null;

        double open = 0.0, close = 0.0;
        Double directRate = null;

        for (FtcItem it : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            try {
                // Jackson 없이 리플렉션/전환 생략: item 필드가 단순하다고 가정
                // 필요한 경우 ObjectMapper.convertValue 사용
                m = (Map<String, Object>) (Object) it;
            } catch (Exception ignored) {}

            for (Map.Entry<String, Object> e : m.entrySet()) {
                String k = (e.getKey() == null ? "" : e.getKey()).toLowerCase();
                Double v = toDouble(e.getValue());
                if (v == null) continue;

                if (k.contains("rate") || k.contains("rt") || k.contains("폐업률")) {
                    if (directRate == null) directRate = v;
                } else if (k.contains("clsbiz") || k.contains("close")) {
                    close += v;
                } else if (k.contains("opbiz") || k.contains("open") || k.contains("new")) {
                    open += v;
                }
            }
        }
        if (directRate != null) return directRate;
        if (open + close > 0) return (close / (open + close)) * 100.0;
        return null;
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            String s = String.valueOf(o);
            if (s.isBlank()) return null;
            return Double.parseDouble(s.replaceAll("[^0-9.\\-]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String mapIndustryToCode(String industry) {
        String s = industry == null ? "" : industry;
        if (s.contains("카페") || s.contains("음식") || s.contains("식당") || s.contains("요식")) return "01";
        if (s.contains("소매") || s.contains("편의점") || s.contains("리테일") || s.contains("마트")) return "02";
        return "03";
    }

    /* ===== 트리 스캔 유틸 ===== */

    private Double firstNumber(JsonNode node, String... keysContains) {
        for (String k : keysContains) {
            Double v = findNumberByKey(node, k);
            if (v != null) return v;
        }
        return null;
    }

    private double sumNumbers(JsonNode node, String... keysContains) {
        double sum = 0.0;
        boolean hit = false;
        for (String k : keysContains) {
            double v = sumNumbersByKey(node, k);
            if (!Double.isNaN(v)) { sum += v; hit = true; }
        }
        return hit ? sum : 0.0;
    }

    private Double findNumberByKey(JsonNode node, String keyContains) {
        final Double[] found = { null };
        traverse(node, (k, n) -> {
            if (k != null && k.toLowerCase().contains(keyContains.toLowerCase()) && n.isValueNode()) {
                try {
                    double v = Double.parseDouble(n.asText().replaceAll("[^0-9.\\-]", ""));
                    if (found[0] == null) found[0] = v;
                } catch (Exception ignored) {}
            }
        });
        return found[0];
    }

    private double sumNumbersByKey(JsonNode node, String keyContains) {
        final double[] sum = { 0.0 };
        final boolean[] hit = { false };
        traverse(node, (k, n) -> {
            if (k != null && k.toLowerCase().contains(keyContains.toLowerCase()) && n.isValueNode()) {
                try {
                    sum[0] += Double.parseDouble(n.asText().replaceAll("[^0-9.\\-]", ""));
                    hit[0] = true;
                } catch (Exception ignored) {}
            }
        });
        return hit[0] ? sum[0] : Double.NaN;
    }

    private interface NodeVisitor { void visit(String key, JsonNode node); }
    private void traverse(JsonNode node, NodeVisitor v) {
        if (node == null) return;
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(fn -> { v.visit(fn, node.get(fn)); traverse(node.get(fn), v); });
        } else if (node.isArray()) {
            for (JsonNode n : node) traverse(n, v);
        }
    }

    private Double round1(Double d) {
        if (d == null) return null;
        return Math.round(d * 10.0) / 10.0;
    }

    private Double defaultRateByLcls(String lcls) {
        // 매우 보수적인 대분류 기본치(최후의 보루)
        return switch (lcls) {
            case "01" -> 8.0; // 외식
            case "02" -> 5.0; // 도소매
            default -> 7.0;   // 서비스
        };
    }

    /* 컨트롤러 반환 타입 */
    public record ClosureRateRecord(String region, String industry, Integer year, Double value, String source) {}
    public record YearRate(Integer year, Double value) {}
}
