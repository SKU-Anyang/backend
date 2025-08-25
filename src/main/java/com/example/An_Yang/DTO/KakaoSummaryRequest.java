package com.example.An_Yang.DTO;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KakaoSummaryRequest {
    private String keyword;
    private Double lat;
    private Double lng;
    private Integer radiusMeters;
    private Integer top; // 요약에 포함할 상위 N개
}