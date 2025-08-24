package com.example.An_Yang.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndustrySimilarityService {

    private final KakaoLocalService kakao;

    /** 좌표 주변에서 seed 키워드로 검색해, 많이 등장한 '중분류' 업종명을 상위 N개 반환 */
    public Mono<List<String>> inferSimilarByKakao(double lat, double lng, int radius, String seed, int topN) {
        return kakao.searchKeywordAll(lat, lng, radius, seed)
                .map(list -> list.stream()
                        .map(p -> Optional.ofNullable(p.category_name()).orElse("")) // "음식점 > 카페 > 커피전문점"
                        .map(s -> {
                            String[] t = s.split(">");
                            return t.length >= 2 ? t[1].trim() : s.trim(); // 중분류 기준
                        })
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                )
                .map(freq -> freq.entrySet().stream()
                        .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(Math.max(1, topN))
                        .map(Map.Entry::getKey)
                        .toList()
                );
    }
}
