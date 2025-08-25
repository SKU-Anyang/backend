package com.example.An_Yang.DTO;

import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskAnalysisResponse {
    private String keyword;
    private String riskLevel;    // LOW / MEDIUM / HIGH
    private String aiSummary;
}