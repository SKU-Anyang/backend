// src/main/java/com/example/An_Yang/controller/InsightController.java
package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.AgeShare;
import com.example.An_Yang.DTO.CompetitorSummary;
import com.example.An_Yang.DTO.LivingPopPoint;
import com.example.An_Yang.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final SeoulLivingPopService seoul;
    private final MoisResidentService mois;
    private final KakaoLocalService kakao;
    private final CompetitorCountService competitorCountService; // ✅ 새 서비스 주입

    /** 시간대별 생활인구 (옵션: targetAge="20대") */
    @GetMapping("/living-pop")
    public Mono<List<LivingPopPoint>> livingPop(
            @RequestParam String date,                  // yyyymmdd (예: 20250701)
            @RequestParam(required = false) String admCd,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String targetAge
    ) {
        Mono<String> adm = resolveAdmCd(admCd, lat, lng);
        return adm.flatMap(code -> seoul.hourlyByDong(date, code, targetAge));
    }

    /** 반경 내 유사 업종 수 (카카오 category_group_code: CE7/FD6 등) */
    @GetMapping("/competitors")
    public Mono<CompetitorSummary> competitors(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "800") int radius,
            @RequestParam(name = "code", required = false) List<String> codes,
            @RequestParam(name = "cat",  required = false) List<String> cat // 호환용
    ) {
        List<String> all = new ArrayList<>();
        if (codes != null) all.addAll(codes);
        if (cat   != null) all.addAll(cat);
        return competitorCountService.countByKakao(lat, lng, radius, all);
    }

    /** 행정동 연령대 비율 (yyyymm) */
    @GetMapping("/age-share")
    public Mono<List<AgeShare>> ageShare(
            @RequestParam String yyyymm,                // 예: 202507
            @RequestParam(required = false) String admCd,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        Mono<String> adm = resolveAdmCd(admCd, lat, lng);
        return adm.flatMap(code -> mois.ageShareByDong(yyyymm, code));
    }

    private Mono<String> resolveAdmCd(String admCd, Double lat, Double lng) {
        if (admCd != null && !admCd.isBlank()) return Mono.just(admCd);
        if (lat != null && lng != null) return kakao.toLegalDongCode(lat, lng);
        return Mono.error(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "필수 파라미터 누락: admCd 또는 (lat,lng) 중 하나는 반드시 제공해야 합니다."
        ));
    }
}
