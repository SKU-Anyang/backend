package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.FtcItem;
import com.example.An_Yang.service.FtcNationwideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ftc")
public class FtcController {

    private final FtcNationwideService ftc;

    /** 최신 연도 폐업률 숫자만(FTC→AI→DEFAULT) */
    @GetMapping("/closure-rate")
    public Mono<FtcNationwideService.ClosureRateRecord> closureRate(
            @RequestParam String region,
            @RequestParam String category
    ) {
        return Mono.fromCallable(() -> ftc.safeGetClosureRate(category, region));
    }

    /** 연도별 폐업률 시리즈(차트용, FTC→AI→DEFAULT) */
    @GetMapping("/closure-rate/series")
    public Mono<List<FtcNationwideService.YearRate>> closureRateSeries(
            @RequestParam String region,
            @RequestParam String category,
            @RequestParam(required = false) Integer start,   // 예: 2020
            @RequestParam(required = false) Integer end      // 예: 2024
    ) {
        return Mono.fromCallable(() -> ftc.getClosureRateSeries(category, region, start, end));
    }

    // ===== 원자료 조회 (디버그/검증용) =====
    @GetMapping("/outfood")
    public Mono<List<FtcItem>> outfood(@RequestParam int yr,
                                       @RequestParam(defaultValue = "1") int pageNo,
                                       @RequestParam(defaultValue = "50") int numOfRows) {
        return ftc.getOutfood(yr, pageNo, numOfRows);
    }

    @GetMapping("/retail")
    public Mono<List<FtcItem>> retail(@RequestParam int yr,
                                      @RequestParam(defaultValue = "1") int pageNo,
                                      @RequestParam(defaultValue = "50") int numOfRows) {
        return ftc.getRetail(yr, pageNo, numOfRows);
    }

    @GetMapping("/service")
    public Mono<List<FtcItem>> service(@RequestParam int yr,
                                       @RequestParam(defaultValue = "1") int pageNo,
                                       @RequestParam(defaultValue = "50") int numOfRows) {
        return ftc.getService(yr, pageNo, numOfRows);
    }

    @GetMapping("/outfood/raw")
    public Mono<String> outfoodRaw(@RequestParam int yr,
                                   @RequestParam(defaultValue = "1") int pageNo,
                                   @RequestParam(defaultValue = "50") int numOfRows) {
        return ftc.rawOutfood(yr, pageNo, numOfRows);
    }
}
