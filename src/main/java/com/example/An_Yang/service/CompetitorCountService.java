// src/main/java/com/example/An_Yang/service/CompetitorCountService.java
package com.example.An_Yang.service;

import com.example.An_Yang.DTO.CompetitorSummary;
import com.example.An_Yang.DTO.KakaoPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompetitorCountService {

    private final KakaoLocalService kakao;

    /** CE7/FD6 등 카카오 category_group_code로 반경 내 점포를 모아 '중분류' 기준으로 개수 집계 */
    public Mono<CompetitorSummary> countByKakao(double lat, double lng, int radius, List<String> codes) {
        List<String> useCodes = (codes == null || codes.isEmpty())
                ? List.of("FD6", "CE7") : codes;

        return Flux.fromIterable(useCodes)
                .concatMap(code -> kakao.searchCategoryAll(lat, lng, radius, code)) // Mono<List<KakaoPlace>>
                .flatMapIterable(list -> list)                                     // KakaoPlace 스트림
                .collectList()                                                     // 전부 모아서
                .map(list -> {
                    Map<String, Integer> by = new LinkedHashMap<>();
                    for (KakaoPlace p : list) {
                        String s = Optional.ofNullable(p.category_name()).orElse(""); // "음식점 > 카페 > 커피전문점"
                        String[] t = s.split(">");
                        String mid = t.length >= 2 ? t[1].trim() : s.trim();          // 중분류
                        if (!mid.isBlank()) by.merge(mid, 1, Integer::sum);
                    }
                    int total = by.values().stream().mapToInt(Integer::intValue).sum();
                    return CompetitorSummary.builder()
                            .total(total)
                            .byCategory(by)
                            .build();
                });
    }
}
