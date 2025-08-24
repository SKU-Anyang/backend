package com.example.An_Yang.service;

import com.example.An_Yang.DTO.KakaoSummaryRequest;
import com.example.An_Yang.DTO.KakaoSummaryResponse;
import com.example.An_Yang.DTO.KakaoSummaryResponse.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KakaoSummaryService {

    private final AiFacade ai;

    public Mono<KakaoSummaryResponse> summarizeMono(KakaoSummaryRequest req, List<Item> all) {
        // 방어 코딩
        List<Item> safeAll = (all == null) ? List.of() : all;
        int radius = (req.getRadiusMeters() == null) ? 1000 : req.getRadiusMeters();
        int top = (req.getTop() == null) ? 10 : Math.max(1, req.getTop());

        // ★원본 리스트 수정하지 않음: 정렬은 새 리스트로 처리
        List<Item> sorted = safeAll.stream()
                .sorted(Comparator.comparingDouble(Item::getDistanceMeters))
                .collect(Collectors.toList());

        int limit = Math.min(top, sorted.size());
        List<Item> pick = new ArrayList<>(sorted.subList(0, limit)); // 가변 복사

        StringBuilder sb = new StringBuilder();
        sb.append("키워드: ").append(req.getKeyword()).append("\n");
        sb.append("반경: ").append(radius).append("m\n");
        sb.append("상위 ").append(pick.size()).append("개 점포:\n");
        for (int i = 0; i < pick.size(); i++) {
            Item it = pick.get(i);
            sb.append("- ").append(i + 1).append(". ").append(nullToDash(it.getName()))
                    .append(" [").append(nullToDash(it.getCategory())).append("], ")
                    .append(String.format("%.0f", it.getDistanceMeters())).append("m, ")
                    .append(nullToDash(it.getAddress()))
                    .append("\n");
        }

        return ai.summarizeMono(
                "유사 점포 현황 요약",
                sb.toString(),
                "포화도/공백지대/포지셔닝 아이디어를 6줄 이내로."
        ).map(aiSummary -> KakaoSummaryResponse.builder()
                .total(safeAll.size())
                .items(pick)
                .aiSummary(aiSummary)
                .build());
    }

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }
}
