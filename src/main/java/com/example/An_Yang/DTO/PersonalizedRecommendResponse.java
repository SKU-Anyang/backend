package com.example.An_Yang.DTO;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalizedRecommendResponse {
    private String strategy;
    private List<String> suggestedIdeas;
    private String aiSummary;
}