package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.KakaoPlace;
import com.example.An_Yang.DTO.KakaoResp;
import com.example.An_Yang.service.GptService;
import com.example.An_Yang.service.KakaoLocalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kakao")
public class KakaoController {

    private final KakaoLocalService kakao;
    private final GptService gpt; // ★ 단건 분석용

    // 1) 키워드: 한 페이지 원본 응답
    @GetMapping("/keyword/page")
    public Mono<KakaoResp> keywordPage(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String q,
            @RequestParam(defaultValue = "1000") int radius,
            @RequestParam(defaultValue = "1") int page
    ) {
        return kakao.searchKeywordPage(lat, lng, radius, q, page);
    }

    // 2) 카테고리: 한 페이지 원본 응답
    @GetMapping("/category/page")
    public Mono<KakaoResp> categoryPage(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String code, // CE7(카페), FD6(음식점) 등
            @RequestParam(defaultValue = "1000") int radius,
            @RequestParam(defaultValue = "1") int page
    ) {
        return kakao.searchCategoryPage(lat, lng, radius, code, page);
    }

    // 3) 키워드: 모든 페이지 합치기
    @GetMapping("/keyword/all")
    public Mono<List<KakaoPlace>> keywordAll(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String q,
            @RequestParam(defaultValue = "1000") int radius
    ) {
        return kakao.searchKeywordAll(lat, lng, radius, q);
    }

    // 4) 카테고리: 모든 페이지 합치기
    @GetMapping("/category/all")
    public Mono<List<KakaoPlace>> categoryAll(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String code,
            @RequestParam(defaultValue = "1000") int radius
    ) {
        return kakao.searchCategoryAll(lat, lng, radius, code);
    }

    // 5) 좌표 → 주소
    @GetMapping("/addr")
    public Mono<String> addr(@RequestParam double lat, @RequestParam double lng) {
        return kakao.coord2address(lat, lng);
    }

    // 6) ★ 단건 점포 분석: 강점/약점/요약
    @PostMapping("/analyze-one")
    public Mono<String> analyzeOne(@RequestBody AnalyzeOneReq req) {
        String prompt = """
                [ROLE] 유사 점포 경쟁력 분석가
                [점포]
                - 이름: %s
                - 카테고리: %s
                - 주소: %s
                - 기준좌표까지 거리: %sm
                - 특이사항/메모: %s

                [TASK]
                1) 강점(• 3~5개)
                2) 약점(• 3~5개)
                3) 한 줄 요약(이 점포의 포지션)
                - 과장 금지, 관찰 가능한 특징 위주로.
                """.formatted(
                nv(req.getName()),
                nv(req.getCategory()),
                nv(req.getAddress()),
                req.getDistanceMeters() == null ? "-" : String.format("%.0f", req.getDistanceMeters()),
                nv(req.getHighlights())
        );
        return Mono.fromCallable(() -> gpt.chat(prompt))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static String nv(String s) { return s == null ? "-" : s; }

    @Data
    public static class AnalyzeOneReq {
        private String name;
        private String category;
        private String address;
        private Double distanceMeters; // 기준좌표와 거리(선택)
        private String highlights;     // 특징/리뷰요약 등(선택)
    }
}