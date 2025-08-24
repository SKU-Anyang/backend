package com.example.An_Yang.service;

import com.example.An_Yang.DTO.AgeShare;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;   // ✅ 중요: 이 import 확인!
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MoisResidentService {

    private final @Qualifier("moisClient") WebClient moisClient;  // ✅ 어떤 WebClient를 쓸지 명시
    @Value("${apis.mois.serviceKey}") private String key;

    /**
     * 행정동별 주민등록 인구(월말 스냅샷)로 연령대 비율 계산
     * @param yyyymm  예: "202507"
     * @param dongCd  행정(법정)동 코드
     */
    public Mono<List<AgeShare>> ageShareByDong(String yyyymm, String dongCd) {
        // ⚠️ 실제 데이터셋 경로(uddi 또는 datasetId)는 포털 문서의 OpenAPI 탭에서 확인해 교체하세요.
        final String datasetPath = "/15077756/v1/uddi-REPLACE_WITH_DATASET_ID";

        return moisClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(datasetPath)
                        .queryParam("serviceKey", key)
                        .queryParam("page", 1)
                        .queryParam("perPage", 10000)
                        // 컬럼명은 데이터셋에 맞춰 조정: cond[기준연월::EQ], cond[행정동코드::EQ] 등
                        .queryParam("cond[기준연월::EQ]", yyyymm)
                        .queryParam("cond[행정동코드::EQ]", dongCd)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(MoisResidentService::toAgeShares);
    }

    /* ================= 내부 파서 ================= */

    @SuppressWarnings("unchecked")
    private static List<AgeShare> toAgeShares(Map<?, ?> root) {
        List<Map<String, Object>> rows = extractRows(root);
        if (rows.isEmpty()) {
            return List.of(
                    new AgeShare("10대", 0, 0),
                    new AgeShare("20대", 0, 0),
                    new AgeShare("30대", 0, 0),
                    new AgeShare("40대", 0, 0),
                    new AgeShare("50대", 0, 0),
                    new AgeShare("60대", 0, 0)
            );
        }

        Map<String, Integer> bucket = new LinkedHashMap<>();
        bucket.put("10대", 0); bucket.put("20대", 0); bucket.put("30대", 0);
        bucket.put("40대", 0); bucket.put("50대", 0); bucket.put("60대", 0);

        for (Map<String, Object> r : rows) {
            // 데이터셋에 따라 컬럼명이 다를 수 있음 → 가능한 키들을 넓게 매칭
            add(bucket, "10대", r, "만10~14세남자","만10~14세여자","만15~19세남자","만15~19세여자",
                    "10~14세남","10~14세여","15~19세남","15~19세여");
            add(bucket, "20대", r, "만20~24세남자","만20~24세여자","만25~29세남자","만25~29세여자",
                    "20~24세남","20~24세여","25~29세남","25~29세여");
            add(bucket, "30대", r, "만30~34세남자","만30~34세여자","만35~39세남자","만35~39세여자",
                    "30~34세남","30~34세여","35~39세남","35~39세여");
            add(bucket, "40대", r, "만40~44세남자","만40~44세여자","만45~49세남자","만45~49세여자",
                    "40~44세남","40~44세여","45~49세남","45~49세여");
            add(bucket, "50대", r, "만50~54세남자","만50~54세여자","만55~59세남자","만55~59세여자",
                    "50~54세남","50~54세여","55~59세남","55~59세여");
            add(bucket, "60대", r, "만60~64세남자","만60~64세여자","만65~69세남자","만65~69세여자",
                    "60~64세남","60~64세여","65~69세남","65~69세여");
        }

        int total = bucket.values().stream().mapToInt(Integer::intValue).sum();
        List<AgeShare> out = new ArrayList<>(bucket.size());
        for (var e : bucket.entrySet()) {
            double ratio = (total == 0) ? 0.0 : Math.round(e.getValue() * 1000.0 / total) / 10.0; // 소수1자리
            out.add(new AgeShare(e.getKey(), e.getValue(), ratio));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractRows(Map<?, ?> root) {
        for (String k : List.of("data", "rows", "items")) {
            Object v = root.get(k);
            if (v instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map) {
                return (List<Map<String, Object>>) (List<?>) l;
            }
        }
        return List.of();
    }

    private static void add(Map<String, Integer> bucket, String key, Map<String, Object> row, String... cols) {
        int s = 0;
        for (String c : cols) {
            Object v = row.get(c);
            if (v == null) continue;
            try { s += Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
        }
        bucket.merge(key, s, Integer::sum);
    }
}
