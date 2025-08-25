package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.ai.MarketDiagnosisRequest;
import com.example.An_Yang.DTO.ai.MarketDiagnosisRequest.SimplePlace;
import com.example.An_Yang.DTO.MarketDiagnosisResponse;
import com.example.An_Yang.service.KakaoLocalService;
import com.example.An_Yang.service.MarketDiagnosisService;
import com.example.An_Yang.util.Haversine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/diag")
@RequiredArgsConstructor
public class MarketDiagnosisController {

    private final KakaoLocalService kakaoLocalService;
    private final MarketDiagnosisService diagnosisService;

    @PostMapping("/market")
    public Mono<MarketDiagnosisResponse> diagnose(@RequestBody MarketDiagnosisRequest req) {
        int radius = req.getRadiusMeters() == null ? 1000 : req.getRadiusMeters();

        // places를 보내지 않았다면 카카오에서 조회해 채워준다.
        if (req.getPlaces() == null || req.getPlaces().isEmpty()) {
            return kakaoLocalService.searchKeywordAll(
                            req.getLat(), req.getLng(), radius, req.getKeyword())
                    .map(places -> places.stream().map(p -> {
                        double lat = Double.parseDouble(p.y());
                        double lon = Double.parseDouble(p.x());
                        return SimplePlace.builder()
                                .id(p.id())
                                .name(p.place_name())
                                .category(p.category_name())
                                .distanceMeters(Haversine.meters(req.getLat(), req.getLng(), lat, lon))
                                .build();
                    }).toList())
                    .flatMap(mapped -> {
                        req.setPlaces(mapped);
                        return diagnosisService.diagnoseMono(req);
                    });
        }
        // places가 이미 들어온 경우 그대로 계산
        return diagnosisService.diagnoseMono(req);
    }
}
