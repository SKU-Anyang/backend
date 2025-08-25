package com.example.An_Yang.service;

import com.example.An_Yang.DTO.BizDesignRequest;
import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import com.example.An_Yang.repository.IdeaRepository;
import com.example.An_Yang.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IdeaService {

    private final UserRepository userRepository;
    private final IdeaRepository ideaRepository;
    private final ObjectMapper objectMapper;

    /** ★ AI 아이디어를 title+region+industry 기준으로 upsert */
    @Transactional
    public Idea upsertIdeaFromAi(String userId,
                                 String title,
                                 String summary,
                                 String industry,
                                 String region,
                                 String contentJson,
                                 Integer closureYear,
                                 Double closureRate) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String t = nullToDash(title);
        String r = nullToDash(region);
        String ind = nullToDash(industry);

        return ideaRepository.findByTitleAndRegionAndIndustry(t, r, ind)
                .map(exist -> {
                    // 업데이트(요약/콘텐츠/폐업지표 최신화)
                    if (notBlank(summary))    exist.setSummary(trunc(summary, 1000));
                    if (notBlank(contentJson)) exist.setContentJson(contentJson);
                    if (closureYear != null)   exist.setClosureYear(closureYear);
                    if (closureRate != null)   exist.setClosureRate(closureRate);
                    return ideaRepository.save(exist);
                })
                .orElseGet(() -> {
                    Idea idea = Idea.builder()
                            .createdBy(user)
                            .title(t)
                            .summary(trunc(summary, 1000))
                            .contentJson(contentJson)
                            .industry(ind)
                            .region(r)
                            .closureYear(closureYear)
                            .closureRate(closureRate)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return ideaRepository.save(idea);
                });
    }

    /** (기존) AI 설계 결과를 Idea로 저장하고 반환 */
    @Transactional
    public Idea saveFromPlan(String userId,
                             BizDesignRequest req,
                             Object ai, // geminiClient.generatePlan(ctx) 결과 객체
                             FtcNationwideService.ClosureRateRecord closure) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String title = extractTitle(ai, req);
        String summary = buildSummary(ai);
        String contentJson = writeJson(ai);

        Idea idea = Idea.builder()
                .createdBy(user)
                .title(nullToDash(title))
                .summary(trunc(summary, 1000))
                .contentJson(contentJson)
                .industry(req.industry())
                .region(req.region())
                .closureYear(closure.year())
                .closureRate(closure.value())
                .createdAt(LocalDateTime.now())
                .build();

        return ideaRepository.save(idea);
    }

    /* ---------- 이하 기존 유틸/헬퍼 ---------- */

    private String extractTitle(Object ai, BizDesignRequest req) {
        try {
            List<?> nameIdeas = (List<?>) ai.getClass().getMethod("getNameIdeas").invoke(ai);
            if (nameIdeas != null && !nameIdeas.isEmpty()) return String.valueOf(nameIdeas.get(0));
        } catch (Exception ignored) { }
        return req.industry() + " 아이디어";
    }

    private String buildSummary(Object ai) {
        String pos = getString(ai, "getPositioning");
        String tgt = getString(ai, "getTargeting");
        String diff = getString(ai, "getDifferentiation");
        return String.format("포지셔닝:%s | 타깃:%s | 차별화:%s",
                trunc(pos, 100), trunc(tgt, 120), trunc(diff, 120));
    }

    private String getString(Object ai, String getter) {
        try {
            Object v = ai.getClass().getMethod(getter).invoke(ai);
            return v == null ? "-" : String.valueOf(v);
        } catch (Exception e) {
            return "-";
        }
    }

    private String writeJson(Object ai) {
        try { return objectMapper.writeValueAsString(ai); }
        catch (Exception e) { return null; }
    }

    private static String trunc(String s, int n) {
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
