package com.example.An_Yang.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketDiagnosisResponse {
    private String keyword;
    private int radiusMeters;
    private int competitorCount;
    private double densityPerKm2;
    private Double competitionRate;  // %  (옵션: 공공데이터/추정)
    private Double closureRate;      // %  (옵션: 공공데이터/추정)
    private String aiSummary;        // GPT 요약
}
