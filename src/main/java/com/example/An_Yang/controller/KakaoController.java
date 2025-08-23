package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.KakaoPlace;
import com.example.An_Yang.DTO.KakaoResp;
import com.example.An_Yang.service.KakaoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kakao")
public class KakaoController {

    private final KakaoLocalService kakao;

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
}
