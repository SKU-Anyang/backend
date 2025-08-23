package com.example.An_Yang.DTO;

public record FtcItem(
        String yr,                 // 연도
        String indutyLclasNm,      // 업종 대분류명 (외식/도소매/서비스)
        String indutyMlsfcNm,      // 업종 중분류명 (치킨/커피/분식/편의점...)
        Integer allFrcsCnt,        // 전체 가맹점 수
        Integer newFrcsRgsCnt,     // 신규 등록 수
        Double  newFrcsRt,         // 개점률(%)
        Double  bfyrNewFrcsRt,     // 전년 개점률
        Double  bfyrVersusNewDffrncRt, // 전년 대비 개점률 증감
        Integer endCncltnFrcsCnt,  // 폐점 수
        Double  endCncltnRt,       // 폐점률(%)
        Double  bfyrVersusEndCncltnRt, // 전년 폐점률
        Double  bfyrVersusDffrncRt     // 전년 대비 폐점률 증감
) {}
