package com.example.An_Yang.service;

import com.example.An_Yang.DTO.CompetitorSummary;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;     // ✅ 이 import 필수
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SbizStoreService {

    private final @Qualifier("sbizClient") WebClient sbizClient;   // ✅ 어떤 WebClient 쓸지 명시
    @Value("${apis.sbiz.serviceKey}") private String key;

    /**
     * 반경 내 점포 수 집계 (분류코드 필터 선택)
     * - data.go.kr 소상공인 상가(상권)정보 OpenAPI: /storeListInRadius
     * - cx: 경도(lng), cy: 위도(lat), radius: 미터
     */
    public Mono<CompetitorSummary> countInRadius(double lat, double lng, int radiusM,
                                                 @Nullable List<String> catCodes) {

        return sbizClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/storeListInRadius")
                            .queryParam("serviceKey", key)
                            .queryParam("type", "json")
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 1000)
                            .queryParam("cx", lng)       // ⚠️ API는 경도를 cx, 위도를 cy 로 받음
                            .queryParam("cy", lat)
                            .queryParam("radius", radiusM);
                    // 분류코드 필터 (없으면 파라미터 미전달)
                    if (catCodes != null && !catCodes.isEmpty()) {
                        // 가이드에 맞는 파라미터로 교체하세요(indsLclsCd/indsMclsCd/indsSclsCd 중 선택)
                        b.queryParam("indsMclsCd", String.join(",", catCodes));
                    }
                    return b.build();
                })
                .retrieve()
                .bodyToMono(Map.class)
                .map(SbizStoreService::toSummary);
    }

    @SuppressWarnings("unchecked")
    private static CompetitorSummary toSummary(Map<?, ?> body) {
        // 응답 구조가 버전에 따라 다를 수 있어 유연 파싱
        List<Map<String, Object>> items = findItems(body);

        Map<String, Integer> byCat = new LinkedHashMap<>();
        for (Map<String, Object> it : items) {
            // 카테고리 이름 후보 키
            String name =
                    asStr(it.get("indsSclsNm"),  // 소분류명
                            asStr(it.get("indsMclsNm"),  // 중분류명
                                    "기타"));
            byCat.merge(name, 1, Integer::sum);
        }

        return CompetitorSummary.builder()
                .total(items.size())
                .byCategory(byCat)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> findItems(Map<?, ?> root) {
        // body/items, response/storeList 등 다양한 래핑 케이스 대응
        for (String k1 : new String[]{"body","bodyVO","response","service","storeListInRadius","data"}) {
            Object o1 = root.get(k1);
            if (o1 instanceof Map<?, ?> m1) {
                for (String k2 : new String[]{"items","item","list","rows","storeList"}) {
                    Object o2 = m1.get(k2);
                    if (o2 instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map) {
                        return (List<Map<String, Object>>) o2;
                    }
                }
            }
        }
        // 최상위가 바로 배열인 경우
        if (root instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map) {
            return (List<Map<String, Object>>) (List<?>) l;
        }
        return List.of();
    }

    private static String asStr(Object v, String dflt) {
        return (v == null) ? dflt : String.valueOf(v);
    }
}
