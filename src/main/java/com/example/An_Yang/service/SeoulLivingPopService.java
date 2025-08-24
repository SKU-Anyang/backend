package com.example.An_Yang.service;

import com.example.An_Yang.DTO.LivingPopPoint;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeoulLivingPopService {

    private final @Qualifier("seoulClient") WebClient seoulClient;   // ✅ 정확한 WebClient 지정
    @Value("${apis.seoul.key}") private String key;                  // 환경변수
    @Value("${apis.seoul.svc.livingByDong}") private String svc;     // 예: SPOP_LOCAL_RESD_DONG

    /**
     * 날짜(yyyymmdd) + 행정동코드(필수) + 시간대코드(선택) 호출
     * 시간대 미지정: 하루 모든 시간대 반환
     */
    public Mono<List<LivingPopPoint>> hourlyByDong(String yyyymmdd, String dongCode, @Nullable String tmzonPdSe) {
        final String path = (tmzonPdSe == null || tmzonPdSe.isBlank())
                // 날짜+동 (시간대 생략 → 빈 세그먼트 // 필수)
                ? String.format("/%s/json/%s/1/1000/%s//%s", key, svc, yyyymmdd, dongCode)
                // 날짜+시간대+동
                : String.format("/%s/json/%s/1/1000/%s/%s/%s", key, svc, yyyymmdd, tmzonPdSe, dongCode);

        return seoulClient.get()               // ✅ seoulClient 사용
                .uri(path)
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    Object boxObj = body.get(svc);
                    if (!(boxObj instanceof Map<?, ?> box)) return List.<LivingPopPoint>of();
                    Object rowsObj = box.get("row");
                    if (!(rowsObj instanceof List<?> rawRows)) return List.<LivingPopPoint>of();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) (List<?>) rawRows;

                    return rows.stream()
                            .sorted(Comparator.comparing(m -> String.valueOf(m.getOrDefault("TMZON_PD_SE", ""))))
                            .map(m -> LivingPopPoint.builder()
                                    .time(mapTimeLabel(String.valueOf(m.getOrDefault("TMZON_PD_SE", ""))))
                                    .total(asDouble(m.getOrDefault("TOT_LVPOP_CO", 0)))
                                    // 연령 분해 컬럼이 없으므로 target은 null 유지
                                    .build())
                            .toList();
                });
    }

    private static double asDouble(Object v) {
        try { return Double.parseDouble(String.valueOf(v)); }
        catch (Exception e) { return 0d; }
    }

    /** 시간대 코드 → 라벨(예: "06" → "06-09") */
    private static String mapTimeLabel(String code) {
        if (code != null && code.matches("\\d{2}")) {
            int hh = Integer.parseInt(code);
            int hh2 = (hh + 3) % 24;
            return String.format("%02d-%02d", hh, hh2);
        }
        return code == null ? "" : code; // 알 수 없으면 원문 유지
    }
}
