package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.ai.*;
import com.example.An_Yang.service.BookmarkService;
import com.example.An_Yang.service.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GptService gptService;
    private final BookmarkService bookmarkService; // ★ 추가

    @PostMapping("/idea")
    public ResponseEntity<String> idea(@RequestBody IdeaRequest req) {
        String prompt = ""
                + "[ROLE] 지역 창업 아이디어 추천 어드바이저\n"
                + "[INPUT]\n"
                + "- 좌표/지역: " + req.region + " (" + req.lat + ", " + req.lng + ")\n"
                + "- 관심사: " + String.valueOf(req.interests) + "\n"
                + "- 반경(m): " + String.valueOf(req.radiusMeters) + "\n"
                + "[INSTRUCTIONS]\n"
                + "- 상권 특성에 맞는 소규모 창업 아이디어 5개\n"
                + "- 각 아이디어는 '핵심가치·타깃·상품/서비스·운영포인트·차별화'를 bullet로 5줄\n"
                + "- 지나친 과장 금지, 실행 가능성 중심\n";
        String out = gptService.chat(prompt);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/market")
    public ResponseEntity<String> market(@RequestBody MarketDiagnosisRequest req) {
        String data = ""
                + "[시장성 데이터]\n"
                + "- 좌표: (" + req.getLat() + ", " + req.getLng() + "), 반경(m): "
                + (req.getRadiusMeters() == null ? "-" : req.getRadiusMeters()) + "\n"
                + "- 업종 키워드: " + req.getKeyword() + " / 연도: " + req.getYear()
                + " / 업종코드: " + req.getIndutyLclsCd() + "\n"
                + "- 경쟁률(선택): " + (req.getCompetitionRate() == null ? "-" : req.getCompetitionRate()) + "\n"
                + "- 폐업률(선택): " + (req.getClosureRate() == null ? "-" : req.getClosureRate()) + "\n"
                + "- 후보 매장 수: " + (req.getPlaces() == null ? 0 : req.getPlaces().size()) + "\n";

        String ask = "위 데이터를 근거로 '" + req.getKeyword()
                + "' 카테고리의 시장성을 1)요약 2)강점 3)리스크 4)권고안(3개)로 15줄 이내로.";

        String out = gptService.chat(data + "\n" + ask);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/design")
    public ResponseEntity<String> design(@RequestBody DesignRequest req) {
        String prompt = ""
                + "[ROLE] 브랜딩/운영 설계 어드바이저\n"
                + "[INPUT] 업종=" + req.category
                + ", 콘셉트힌트=" + req.conceptHint
                + ", 톤=" + req.tone
                + ", 타깃=" + req.target + "\n"
                + "[TASK] 상호명 5개, 콘셉트 문장, 인테리어 키워드, 시그니처 메뉴/서비스, 운영 체크리스트(10개)";
        String out = gptService.chat(prompt);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/nearby")
    public ResponseEntity<String> nearby(@RequestBody NearbyRequest req) {
        List<String> list = (req.competitorSummaries == null ? java.util.List.of() : req.competitorSummaries);
        String joined = String.join("\n- ", list);
        String prompt = ""
                + "[ROLE] 경쟁점 요약 분석가\n"
                + "[지역] " + req.region + " / [업종] " + req.category + "\n"
                + "[경쟁점 요약]\n"
                + "- " + joined + "\n\n"
                + "[OUTPUT] 공통강점·공통약점·틈새기회(3개)·피해야 할 포지션(3개)";
        String out = gptService.chat(prompt);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/closure-risk")
    public ResponseEntity<String> closureRisk(@RequestBody ClosureRiskRequest req) {
        String data = ""
                + "[폐업 데이터]\n"
                + "- 폐업률(%): " + req.closureRate + "\n"
                + "- 추세기울기: " + req.trendSlope + "\n";
        String ask = "위 자료를 근거로 '" + req.region + "·" + req.category
                + "'의 폐업 리스크 수준(낮음/보통/높음)과 원인 가설, 완화전략 5개를 제시.";
        String out = gptService.chat(data + "\n" + ask);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/boss-assistant")
    public ResponseEntity<String> boss(@RequestBody BossAssistantRequest req) {
        String sys = "너는 매장 운영 B2B 도우미로서 간결하고 실행 가능한 답을 제공한다.";
        String user = ""
                + "[매장] " + req.storeName + " (" + req.category + ")\n"
                + "[요청] " + req.task + "\n"
                + "[맥락] " + req.context + "\n"
                + "[톤] " + req.tone + "\n";
        String out = gptService.chat(sys, user);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/curation")
    public ResponseEntity<String> curation(@RequestBody CurationRequest req) {
        List<String> liked = (req.likedIdeaSummaries == null ? java.util.List.of() : req.likedIdeaSummaries);
        String joined = String.join("\n- ", liked);
        String prompt = ""
                + "[ROLE] 맞춤형 큐레이션 엔진\n"
                + "[지역] " + req.region + "\n"
                + "[프로필] " + req.profile + "\n"
                + "[찜한 아이디어]\n"
                + "- " + joined + "\n\n"
                + "[TASK] 사용자가 좋아할 확률 높은 신규 아이디어 5개와 개인화 이유를 함께 제시.";
        String out = gptService.chat(prompt);
        return ResponseEntity.ok(out);
    }

    /** ★ 내 찜 기반 자동 큐레이션: GET /api/ai/curation/from-bookmarks?region=안양시&profile=... (토큰 필요) */
    @GetMapping("/curation/from-bookmarks")
    public ResponseEntity<String> curationFromBookmarks(
            @RequestParam String region,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false, defaultValue = "5") int take,
            Authentication auth
    ) {
        String userId = (String) auth.getPrincipal();
        List<String> likedSummaries = bookmarkService.listSummaries(userId); // 제목/요약 자동 수집
        if (likedSummaries.isEmpty()) {
            return ResponseEntity.ok("찜한 아이디어가 없어 기본 추천을 제시합니다.\n- " + String.join("\n- ", List.of(
                    "로컬 스페셜티 카페", "베이킹 클래스 스튜디오", "펫프렌들리 라운지", "수제 디저트 바", "플라워 DIY 카페"
            )));
        }
        String joined = String.join("\n- ", likedSummaries.stream().limit(Math.max(take, 1)).toList());
        String prompt = """
                [ROLE] 맞춤형 큐레이션 엔진
                [지역] %s
                [프로필] %s
                [찜한 아이디어(샘플 %d개)]
                - %s

                [TASK]
                - 사용자가 좋아할 확률 높은 신규 아이디어 5개
                - 각 아이디어마다 '개념·타깃·상품/서비스·운영포인트·차별화' 5줄
                - 중복/유사 콘셉트는 피하고 다양성 유지
                """.formatted(region, profile == null ? "-" : profile, Math.max(take, 1), joined);
        return ResponseEntity.ok(gptService.chat(prompt));
    }
}
