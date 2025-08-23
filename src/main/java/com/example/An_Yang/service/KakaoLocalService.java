package com.example.An_Yang.service;

import com.example.An_Yang.DTO.KakaoMeta;
import com.example.An_Yang.DTO.KakaoPlace;
import com.example.An_Yang.DTO.KakaoResp;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    // ✅ Qualifier로 정확히 kakaoClient 주입
    private final @Qualifier("kakaoClient") WebClient kakaoClient;

    /** 키워드 검색: 한 페이지 */
    public Mono<KakaoResp> searchKeywordPage(double lat, double lng, int radius, String query, int page) {
        int r = clampRadius(radius);
        int p = clampPage(page);
        int size = 15;     // 필요하면 파라미터화

        return kakaoClient.get().uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("y", lat)           // lat
                        .queryParam("x", lng)           // lng
                        .queryParam("radius", r)        // ✅ 전달값 사용
                        .queryParam("page", p)          // ✅ 전달값 사용 (1~45)
                        .queryParam("size", size)       // 1~45
                        .queryParam("sort", "distance") // 좌표 기반이면 distance 권장
                        .build())
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new RuntimeException(
                                        "[KAKAO] " + resp.statusCode() + " - " + b))))
                .bodyToMono(KakaoResp.class);
    }

    /** 카테고리 검색: 한 페이지 */
    public Mono<KakaoResp> searchCategoryPage(double lat, double lng, int radius, String code, int page) {
        int r = clampRadius(radius);
        int p = clampPage(page);
        int size = 15;

        return kakaoClient.get().uri(uri -> uri.path("/v2/local/search/category.json")
                        .queryParam("category_group_code", code) // CE7, FD6 ...
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", r)   // ✅ 전달값 사용
                        .queryParam("page", p)     // ✅ 전달값 사용
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new RuntimeException(
                                        "[KAKAO] " + resp.statusCode() + " - " + b))))
                .bodyToMono(KakaoResp.class);
    }

    /** 키워드 검색: 모든 페이지 합치기 */
    public Mono<List<KakaoPlace>> searchKeywordAll(double lat, double lng, int radius, String query) {
        int r = clampRadius(radius);
        return fetchAllPages(p -> searchKeywordPage(lat, lng, r, query, p));
    }

    /** 카테고리 검색: 모든 페이지 합치기 */
    public Mono<List<KakaoPlace>> searchCategoryAll(double lat, double lng, int radius, String code) {
        int r = clampRadius(radius);
        return fetchAllPages(p -> searchCategoryPage(lat, lng, r, code, p));
    }

    /** 좌표 → 도로명/지번 주소 */
    public Mono<String> coord2address(double lat, double lng) {
        return kakaoClient.get().uri(uri -> uri.path("/v2/local/geo/coord2address.json")
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .build())
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .flatMap(b -> Mono.error(new RuntimeException(
                                        "[KAKAO] " + resp.statusCode() + " - " + b))))
                .bodyToMono(String.class);
    }

    // ----- 내부 유틸 -----

    /** 첫 페이지를 기준으로 전체 페이지 수 계산 후 순차 수집 */
    private Mono<List<KakaoPlace>> fetchAllPages(Function<Integer, Mono<KakaoResp>> pageFetcher) {
        return pageFetcher.apply(1).flatMap(first -> {
            int total   = Optional.ofNullable(first.meta()).map(KakaoMeta::pageable_count).orElse(0);
            int perPage = Optional.ofNullable(first.documents()).map(List::size).orElse(15);
            if (perPage <= 0) perPage = 15;
            int maxPage = Math.min(45, (int) Math.ceil(total / (double) perPage));
            if (maxPage < 1) maxPage = 1;

            List<KakaoPlace> acc = new ArrayList<>(Optional.ofNullable(first.documents()).orElseGet(List::of));

            Mono<List<KakaoPlace>> merged = (maxPage == 1)
                    ? Mono.just(acc)
                    : Flux.range(2, maxPage - 1)   // 2..maxPage
                    .concatMap(pageFetcher)        // 순차 요청(쿼터 안정)
                    .map(r -> Optional.ofNullable(r.documents()).orElseGet(List::of))
                    .reduce(acc, (list, cur) -> { list.addAll(cur); return list; });

            return merged.map(this::dedupByIdOrNameAddr);
        });
    }

    /** id 우선 중복 제거(없으면 이름+도로명주소 키) */
    private List<KakaoPlace> dedupByIdOrNameAddr(List<KakaoPlace> list) {
        if (list == null || list.isEmpty()) return List.of();
        return new ArrayList<>(list.stream().collect(Collectors.toMap(
                KakaoPlace::uniqueKey, it -> it, (a, b) -> a, LinkedHashMap::new
        )).values());
    }

    private int clampRadius(int radius) { return Math.max(10, Math.min(radius, 20000)); }
    private int clampPage(int page)     { return Math.max(1, Math.min(page, 45)); }
}
