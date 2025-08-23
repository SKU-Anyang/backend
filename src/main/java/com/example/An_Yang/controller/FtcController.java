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

    // 원문 확인용
    @GetMapping("/outfood/raw")
    public Mono<String> outfoodRaw(@RequestParam int yr,
                                   @RequestParam(defaultValue = "1") int pageNo,
                                   @RequestParam(defaultValue = "50") int numOfRows) {
        return ftc.rawOutfood(yr, pageNo, numOfRows);
    }
}
