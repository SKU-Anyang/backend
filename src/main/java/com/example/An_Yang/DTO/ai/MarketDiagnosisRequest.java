package com.example.An_Yang.DTO.ai;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketDiagnosisRequest {
    private String keyword;          // 업종 키워드 (e.g., 카페, 미용실)
    private Double lat;              // 위도
    private Double lng;              // 경도
    private Integer radiusMeters;    // 반경 (기본 1000m)
    private Integer year;            // 공공데이터 기준연도 (선택)
    private String indutyLclsCd;     // 공공데이터 업종코드 (선택)
    // 옵션: 프론트/기타 엔드포인트에서 이미 가져온 Kakao 결과를 넘길 수 있게
    private List<String> categoryCodes;
    private List<SimplePlace> places;
    // 옵션: FTC 사전 계산값 (없으면 내부에서 추정)
    private Double competitionRate;  // 경쟁률(%) 선택
    private Double closureRate;      // 폐업률(%) 선택
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SimplePlace {
        private String id;
        private String name;
        private String category;
        private double distanceMeters;
    }
}