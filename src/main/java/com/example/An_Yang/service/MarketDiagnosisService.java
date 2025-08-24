package com.example.An_Yang.service;

import com.example.An_Yang.DTO.AgeShare;
import com.example.An_Yang.DTO.CompetitorSummary;
import com.example.An_Yang.DTO.LivingPopPoint;
import com.example.An_Yang.DTO.MarketDiagnosisResponse;
import com.example.An_Yang.DTO.ai.MarketDiagnosisRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketDiagnosisService {

    // 의존성 주입
    private final AiFacade ai;
    private final SbizStoreService sbiz;
    private final SeoulLivingPopService seoul;
    private final MoisResidentService mois;
    private final KakaoLocalService kakao;

    /**
     * 경쟁점포 / 생활인구(합계+타깃) / 연령비율을 모아 AI 요약을 반환
     */
    public Mono<MarketDiagnosisResponse> diagnoseMono(MarketDiagnosisRequest req) {
        final int radius = (req.getRadiusMeters() == null) ? 1000 : req.getRadiusMeters();

        // 1) 날짜/월 기본값 (서울 생활인구 5일 지연)
        final ZoneId KST = ZoneId.of("Asia/Seoul");
        final String date  = LocalDate.now(KST).minusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE); // yyyymmdd
        final String month = date.substring(0, 6);                                                      // yyyymm

        // 2) 행정동코드: 좌표 → 법정동코드
        Mono<String> adm = kakao.toLegalDongCode(req.getLat(), req.getLng());

        // 3) 외부 데이터 병렬 호출
        Mono<CompetitorSummary> comp =
                sbiz.countInRadius(req.getLat(), req.getLng(), radius, req.getCategoryCodes());

        // ⚠️ 네 SeoulLivingPopService 시그니처에 맞춰 아래 중 하나로:
        //  (A) 3번째 인자가 "타깃 연령"이라면 "20대" 전달
        Mono<List<LivingPopPoint>> live =
                adm.flatMap(code -> seoul.hourlyByDong(date, code, "20대"));

        //  (B) 3번째 인자가 "시간대 코드"라면 null 전달(하루 전체)하고,
        //      20대 라인은 별도의 연령분해 서비스에서 계산해 병합하세요.
        // Mono<List<LivingPopPoint>> live =
        //        adm.flatMap(code -> seoul.hourlyByDong(date, code, null));

        Mono<List<AgeShare>> ages =
                adm.flatMap(code -> mois.ageShareByDong(month, code));

        // 4) 합치고 AI 요약 생성
        return Mono.zip(comp, live, ages)
                .flatMap(tuple -> {
                    var c  = tuple.getT1();
                    var lp = tuple.getT2();
                    var ag = tuple.getT3();

                    String dataBlock = """
                            [입지 데이터]
                            - 키워드: %s
                            - 반경: %,d m
                            - 경쟁 점포: %,d개 (상세: %s)
                            - 생활인구 피크(전체): %s ~ %s
                            - 타깃(20대) 시간대 평균: %.0f명
                            - 연령대 상위: %s
                            """.formatted(
                            nullToDash(req.getKeyword()),
                            radius,
                            c.getTotal(),
                            top3(c.getByCategory()),
                            firstHour(lp),
                            lastHour(lp),
                            avgTarget(lp),
                            topAges(ag)
                    );

                    return ai.summarizeMono(
                            "시장성/입지 진단 요약",
                            dataBlock,
                            "위 데이터를 근거로 1) 입지 포화도와 경쟁 해석, 2) 위험 요인(폐업 가능성 등), 3) 차별화 포인트 3가지를 한국어로 간결하게 제안해줘."
                    ).map(aiSummary -> MarketDiagnosisResponse.builder()
                            .keyword(req.getKeyword())
                            .radiusMeters(radius)
                            .competitorCount(c.getTotal())
                            .aiSummary(aiSummary)
                            .build());
                });
    }

    /* ==================== 헬퍼 ==================== */

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private static String top3(Map<String, Integer> byCategory) {
        if (byCategory == null || byCategory.isEmpty()) return "-";
        return byCategory.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String firstHour(List<LivingPopPoint> lp) {
        if (lp == null || lp.isEmpty()) return "-";
        var max = lp.stream().max(Comparator.comparingDouble(LivingPopPoint::getTotal)).orElse(lp.get(0));
        return startHour(max.getTime());
    }
    private static String lastHour(List<LivingPopPoint> lp) {
        if (lp == null || lp.isEmpty()) return "-";
        var max = lp.stream().max(Comparator.comparingDouble(LivingPopPoint::getTotal)).orElse(lp.get(0));
        return endHour(max.getTime());
    }
    private static String startHour(String label) {
        if (label == null) return "-";
        String[] p = label.split("-");
        return (p.length > 0) ? p[0] : label;
    }
    private static String endHour(String label) {
        if (label == null) return "-";
        String[] p = label.split("-");
        return (p.length > 1) ? p[1] : label;
    }

    private static double avgTarget(List<LivingPopPoint> lp) {
        if (lp == null || lp.isEmpty()) return 0d;
        double sum = 0; int n = 0;
        for (LivingPopPoint p : lp) {
            if (p.getTarget() != null) { sum += p.getTarget(); n++; }
        }
        return (n == 0) ? 0d : Math.round((sum / n));
    }

    private static String topAges(List<AgeShare> ages) {
        if (ages == null || ages.isEmpty()) return "-";
        return ages.stream()
                .sorted((a, b) -> Double.compare(b.getRatio(), a.getRatio()))
                .limit(2)
                .map(a -> a.getBucket() + " " + round1(a.getRatio()) + "%")
                .collect(Collectors.joining(", "));
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
