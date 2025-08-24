// service/IdeaService.java
package com.example.An_Yang.service;

import com.example.An_Yang.DTO.BizDesignRequest;
import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import com.example.An_Yang.repository.IdeaRepository;
import com.example.An_Yang.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdeaService {

    private final UserRepository userRepository;
    private final IdeaRepository ideaRepository;
    private final ObjectMapper objectMapper;

    /** AI 설계 결과를 Idea로 저장하고 반환 */
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
                .summary(nullToDash(summary))
                .contentJson(contentJson)
                .industry(req.industry())
                .region(req.region())
                .closureYear(closure.year())
                .closureRate(closure.value())
                .createdAt(LocalDateTime.now())
                .build();

        return ideaRepository.save(idea);
    }

    private String extractTitle(Object ai, BizDesignRequest req) {
        try {
            // ai.getNameIdeas() 가 List<String> 이라고 가정
            List<?> nameIdeas = (List<?>) ai.getClass().getMethod("getNameIdeas").invoke(ai);
            if (nameIdeas != null && !nameIdeas.isEmpty()) return String.valueOf(nameIdeas.get(0));
        } catch (Exception ignored) { }
        // fallback
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
        if (s == null) return "-";
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
    private static String nullToDash(String s) { return s == null ? "-" : s; }
}
