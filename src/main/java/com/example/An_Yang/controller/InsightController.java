package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.AgeShare;
import com.example.An_Yang.DTO.CompetitorSummary;
import com.example.An_Yang.DTO.LivingPopPoint;
import com.example.An_Yang.service.KakaoLocalService;
import com.example.An_Yang.service.MoisResidentService;
import com.example.An_Yang.service.SbizStoreService;
import com.example.An_Yang.service.SeoulLivingPopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

// InsightController.java
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final SeoulLivingPopService seoul;
    private final SbizStoreService sbiz;
    private final MoisResidentService mois;
    private final KakaoLocalService kakao; // 좌표 -> 법정동코드 보조 (추가 메서드 아래 참고)

    /** 시간대별 생활인구 (옵션: targetAge="20대") */
    @GetMapping("/living-pop")
    public Mono<List<LivingPopPoint>> livingPop(
            @RequestParam String date,           // yyyymmdd
            @RequestParam(required=false) String admCd,
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(required=false) String targetAge
    ){
        Mono<String> adm = (admCd!=null)? Mono.just(admCd) : kakao.toLegalDongCode(lat, lng);
        return adm.flatMap(code -> seoul.hourlyByDong(date, code, targetAge));
    }

    /** 반경 내 유사 업종 카운트 (카페·디저트 등) */
    @GetMapping("/competitors")
    public Mono<CompetitorSummary> competitors(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue="800") int radius,
            @RequestParam(required=false) List<String> cat // 업종코드 리스트
    ){
        return sbiz.countInRadius(lat, lng, radius, cat);
    }

    /** 행정동 연령대 비율 (yyyymm 기준) */
    @GetMapping("/age-share")
    public Mono<List<AgeShare>> ageShare(
            @RequestParam String yyyymm,
            @RequestParam(required=false) String admCd,
            @RequestParam double lat, @RequestParam double lng
    ){
        Mono<String> adm = (admCd!=null)? Mono.just(admCd) : kakao.toLegalDongCode(lat, lng);
        return adm.flatMap(code -> mois.ageShareByDong(yyyymm, code));
    }
}

