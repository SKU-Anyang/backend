package com.example.An_Yang.DTO;

import java.util.List;

public record BizDesignResponse(
        BizDesign design,
        Risk risk,
        Metrics metrics
) {
    public record BizDesign(String positioning, String targeting, String differentiation,
                            String revenue, List<String> nameIdeas) {}
    public record Risk(List<RiskItem> major) {}
    public record RiskItem(String title, String why, int severity, String mitigation) {}
    public record Metrics(ClosureRate closureRate, Competition competition, AvgRent avgRent) {}
    public record ClosureRate(String region, String industry, Integer year, Double value, String source) {}
    public record Competition(Integer poi, Integer radiusM) {}
    public record AvgRent(Integer pyeong, String source) {}
}
