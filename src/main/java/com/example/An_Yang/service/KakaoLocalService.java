package com.example.An_Yang.service;

import com.example.An_Yang.DTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class KakaoLocalService {

    // KakaoApiConfig에서 만든 WebClient 주입 (Authorization: KakaoAK {REST_KEY})
    private final @Qualifier("kakaoClient") WebClient kakaoClient;

    /* ======================= Local(장소) 검색 ======================= */

    /** 키워드 검색: 한 페이지 */
    public Mono<KakaoResp> searchKeywordPage(double lat, double lng, int radius, String query, int page) {
        int r = clampRadius(radius);
        int p = clampPage(page);
        int size = 15;

        return kakaoClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", r)
                        .queryParam("page", p)
                        .queryParam("size", size)
                        .queryParam("sort", "distance")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        resp.statusCode(),
                                        "[KAKAO keyword] " + resp.statusCode() + " - " + trim(body)
                                ))))
                .bodyToMono(KakaoResp.class);
    }

    /** 카테고리 검색: 한 페이지 */
    public Mono<KakaoResp> searchCategoryPage(double lat, double lng, int radius, String code, int page) {
        int r = clampRadius(radius);
        int p = clampPage(page);
        int size = 15;

        return kakaoClient.get()
                .uri(uri -> uri.path("/v2/local/search/category.json")
                        .queryParam("category_group_code", code) // CE7, FD6 등
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .queryParam("radius", r)
                        .queryParam("page", p)
                        .queryParam("size", size)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        resp.statusCode(),
                                        "[KAKAO category] " + resp.statusCode() + " - " + trim(body)
                                ))))
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

    /** 좌표 → 도로명/지번 주소 (원문 JSON 그대로 반환) */
    public Mono<String> coord2address(double lat, double lng) {
        return kakaoClient.get()
                .uri(uri -> uri.path("/v2/local/geo/coord2address.json")
                        .queryParam("y", lat)
                        .queryParam("x", lng)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        resp.statusCode(),
                                        "[KAKAO coord2address] " + resp.statusCode() + " - " + trim(body)
                                ))))
                .bodyToMono(String.class);
    }

    /** 좌표 → 법정동코드 */
    @SuppressWarnings("unchecked")
    public Mono<String> toLegalDongCode(double lat, double lng) {
        return kakaoClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2regioncode.json")
                        .queryParam("x", lng) // 경도
                        .queryParam("y", lat) // 위도
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        resp.statusCode(),
                                        "[KAKAO coord2regioncode] " + resp.statusCode() + " - " + trim(body)
                                ))))
                .bodyToMono(Map.class)
                .map(m -> {
                    List<Map<String, Object>> docs = (List<Map<String, Object>>) m.get("documents");
                    if (docs == null || docs.isEmpty()) {
                        throw new ResponseStatusException(
                                org.springframework.http.HttpStatus.BAD_GATEWAY,
                                "좌표->법정동코드 실패: 결과 없음");
                    }
                    Map<String, Object> first = docs.stream()
                            .filter(d -> "B".equals(String.valueOf(d.get("region_type")))) // 법정동(B) 우선
                            .findFirst().orElse(docs.get(0));
                    return String.valueOf(first.getOrDefault("code", ""));
                });
    }

    /* ---------- 내부 페이징 유틸 ---------- */
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

    private List<KakaoPlace> dedupByIdOrNameAddr(List<KakaoPlace> list) {
        if (list == null || list.isEmpty()) return List.of();
        return new ArrayList<>(list.stream().collect(Collectors.toMap(
                KakaoPlace::uniqueKey,
                it -> it,
                (a, b) -> a,
                LinkedHashMap::new
        )).values());
    }

    private int clampRadius(int radius) { return Math.max(10, Math.min(radius, 20000)); }
    private int clampPage(int page)     { return Math.max(1, Math.min(page, 45)); }
    private static String trim(String s){ if (s == null) return ""; s = s.trim(); return s.length() > 500 ? s.substring(0, 500) + "..." : s; }

    /* ======================= Image(이미지) 검색 ======================= */

    /** 카카오 이미지 검색: 질의어로 이미지 URL 리스트(원본 image_url)를 반환 */
    public Mono<List<String>> searchImages(String query, int size) {
        int sz = Math.max(1, Math.min(size, 50));
        return kakaoClient.get()
                .uri(uri -> uri.path("/v2/search/image")
                        .queryParam("query", query)
                        .queryParam("size", sz)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        resp.statusCode(),
                                        "[KAKAO image] " + resp.statusCode() + " - " + trim(body)
                                ))))
                .bodyToMono(KakaoImageResp.class)
                .map(r -> Optional.ofNullable(r.documents()).orElseGet(List::of))
                .map(docs -> docs.stream()
                        .map(KakaoImageDoc::image_url)
                        .filter(Objects::nonNull)
                        .toList()
                );
    }

    /** 가게 한 건에 대해 이미지 검색(가게명 + 도로명/지번주소 + 카테고리로 질의 구성) */
    public Mono<KakaoPlaceWithImage> attachFirstImage(KakaoPlace p, int size) {
        String q = buildPlaceQuery(p);
        return searchImages(q, size)
                .defaultIfEmpty(List.of())
                .map(urls -> new KakaoPlaceWithImage(p, urls.isEmpty()? null : urls.get(0), urls));
    }

    /** 가게 리스트에 이미지 붙이기(순차 처리: 쿼터 안정) */
    public Mono<List<KakaoPlaceWithImage>> attachImages(List<KakaoPlace> places, int size) {
        if (places == null || places.isEmpty()) return Mono.just(List.of());
        return Flux.fromIterable(places)
                .concatMap(p -> attachFirstImage(p, size))
                .collectList();
    }

    /** 키워드 결과(모든 페이지) + 이미지까지 한 번에 */
    public Mono<List<KakaoPlaceWithImage>> keywordAllWithImages(double lat, double lng, int radius, String query, int imgSize) {
        return searchKeywordAll(lat, lng, radius, query)
                .flatMap(list -> attachImages(list, imgSize));
    }

    private String buildPlaceQuery(KakaoPlace p) {
        // place_name + road_address(또는 지번주소) + category_name 를 조합
        return Stream.of(p.place_name(), p.road_address_name(), p.address_name(), p.category_name())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(" "));
    }
}