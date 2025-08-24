package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.RiskAnalysisRequest;
import com.example.An_Yang.DTO.RiskAnalysisResponse;
import com.example.An_Yang.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/diag")
@RequiredArgsConstructor
public class RiskAnalysisController {

    private final RiskAnalysisService riskService;

    @PostMapping("/risk")
    public Mono<RiskAnalysisResponse> risk(@RequestBody RiskAnalysisRequest req) {
        // densityPerKm2를 프론트나 별도 API에서 계산해 채워서 보내는 형태
        return riskService.analyzeMono(req);
    }
}
