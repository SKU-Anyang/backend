// controller/PlanController.java
package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.BizDesignRequest;
import com.example.An_Yang.DTO.BizDesignResponse;
import com.example.An_Yang.service.FtcNationwideService;
import com.example.An_Yang.service.GptService;
import com.example.An_Yang.service.IdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class PlanController {

    private final FtcNationwideService ftcService;
    private final GptService geminiClient;
    private final IdeaService ideaService;

    @PostMapping("/plan")
    public ResponseEntity<BizDesignResponse> plan(@RequestBody @Valid BizDesignRequest req,
                                                  Authentication auth) {
        // 1) 폐업률
        var closure = ftcService.safeGetClosureRate(req.industry(), req.region());

        // 2) 컨텍스트
        Map<String, Object> ctx = Map.<String, Object>of(
                "industry",   req.industry(),
                "region",     req.region(),
                "target",     String.join(",", Optional.ofNullable(req.target()).orElseGet(java.util.List::of)),
                "budgetKrw",  req.budgetKrw(),
                "staff",      req.staff(),
                "areaPyeong", req.areaPyeong(),
                "notes",      Optional.ofNullable(req.notes()).orElse(""),
                "closureRate", closure.value() == null ? 0.0 : closure.value()
        );

        // 3) 설계 생성
        var ai = geminiClient.generatePlan(ctx);

        // 4) DB 저장 (로그인 사용자 기준)
        String userId = (String) Optional.ofNullable(auth).map(Authentication::getPrincipal)
                .orElseThrow(() -> new IllegalArgumentException("인증이 필요합니다."));
        var idea = ideaService.saveFromPlan(userId, req, ai, closure);

        // 5) 응답 구성 + ideaId를 헤더로 알려주기
        var body = new BizDesignResponse(
                new BizDesignResponse.BizDesign(
                        ai.getPositioning(), ai.getTargeting(), ai.getDifferentiation(), ai.getRevenue(), ai.getNameIdeas()
                ),
                new BizDesignResponse.Risk(
                        ai.getMajorRisks().stream()
                                .map(r -> new BizDesignResponse.RiskItem(r.getTitle(), r.getWhy(), r.getSeverity(), r.getMitigation()))
                                .toList()
                ),
                new BizDesignResponse.Metrics(
                        new BizDesignResponse.ClosureRate(
                                closure.region(), closure.industry(), closure.year(), closure.value(), closure.source()
                        ),
                        null, null
                )
        );

        return ResponseEntity.ok()
                .header("X-Idea-Id", String.valueOf(idea.getId()))
                .location(URI.create("/api/ideas/" + idea.getId()))
                .body(body);
    }
}
