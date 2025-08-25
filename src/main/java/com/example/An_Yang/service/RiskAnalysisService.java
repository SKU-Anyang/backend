package com.example.An_Yang.service;

import com.example.An_Yang.DTO.RiskAnalysisRequest;
import com.example.An_Yang.DTO.RiskAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RiskAnalysisService {

    private final AiFacade ai;

    public Mono<RiskAnalysisResponse> analyzeMono(RiskAnalysisRequest req) {
        double comp = nz(req.getCompetitionRate());
        double close = nz(req.getClosureRate());
        double dens  = nz(req.getDensityPerKm2());

        // 가중합 스코어링 (0~100)
        double score = clamp(0, 100, comp * 0.5 + close * 0.3 + Math.min(100, dens * 5) * 0.2);
        String level = (score >= 66) ? "HIGH" : (score >= 40) ? "MEDIUM" : "LOW";

        String data = """
                - 키워드: %s
                - 경쟁률: %.1f %%
                - 폐업률: %.1f %%
                - 점포밀도: %.2f 개/km²
                - 종합점수(0~100): %.1f
                """.formatted(req.getKeyword(), comp, close, dens, score);

        return ai.summarizeMono(
                "폐업 리스크 분석",
                data,
                "리스크 레벨에 따른 주의사항과 완화전략을 한국어로 5줄 요약."
        ).map(aiSummary -> RiskAnalysisResponse.builder()
                .keyword(req.getKeyword())
                .riskLevel(level)
                .aiSummary(aiSummary)
                .build());
    }

    private double nz(Double v) { return v == null ? 0.0 : v; }
    private double clamp(double min, double max, double v) { return Math.max(min, Math.min(max, v)); }
}
