package com.example.An_Yang.DTO;

import java.util.List;

/** 카카오 이미지 검색 응답 */
public record KakaoImageResp(
        List<KakaoImageDoc> documents,
        KakaoMeta meta
) {}
