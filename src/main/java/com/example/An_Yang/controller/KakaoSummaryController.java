package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.KakaoSummaryRequest;
import com.example.An_Yang.DTO.KakaoSummaryResponse;
import com.example.An_Yang.DTO.KakaoSummaryResponse.Item;
import com.example.An_Yang.service.KakaoLocalService;
import com.example.An_Yang.service.KakaoSummaryService;
import com.example.An_Yang.util.Haversine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoSummaryController {

    private final KakaoLocalService kakaoLocalService;
    private final KakaoSummaryService summaryService;

    @PostMapping("/summary")
    public Mono<KakaoSummaryResponse> summary(@RequestBody KakaoSummaryRequest req) {
        int radius = req.getRadiusMeters() == null ? 1000 : req.getRadiusMeters();

        // Kakao에서 전체 페이지 수집 → DTO 변환 → ★가변 리스트로 변환해 전달★
        return kakaoLocalService.searchKeywordAll(
                        req.getLat(), req.getLng(), radius, req.getKeyword())
                .map(places -> places.stream()
                        .map(p -> {
                            double lat = Double.parseDouble(p.y()); // 위도
                            double lon = Double.parseDouble(p.x()); // 경도
                            double dist = Haversine.meters(req.getLat(), req.getLng(), lat, lon);
                            return Item.builder()
                                    .name(p.place_name())
                                    .category(p.category_name())
                                    .address(p.road_address_name())
                                    .distanceMeters(dist)
                                    .build();
                        })
                        .collect(Collectors.toCollection(ArrayList::new)) // ✅ 가변 리스트
                )
                .flatMap(items -> summaryService.summarizeMono(req, items));
    }
}
