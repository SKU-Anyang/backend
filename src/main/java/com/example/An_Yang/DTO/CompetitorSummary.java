package com.example.An_Yang.DTO;

import lombok.*;

import java.util.Map;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitorSummary {
    private int total;
    private Map<String,Integer> byCategory; // 예: {"감성 카페":120, ...}
}
