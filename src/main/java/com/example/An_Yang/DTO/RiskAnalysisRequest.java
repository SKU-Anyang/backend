package com.example.An_Yang.DTO;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskAnalysisRequest {
    private String keyword;
    private Double competitionRate; // %
    private Double closureRate;     // %
    private Double densityPerKm2;   // kakao 기반 점포밀도
}

