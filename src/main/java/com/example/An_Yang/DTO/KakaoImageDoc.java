package com.example.An_Yang.DTO;

public record KakaoImageDoc(
        String collection,
        String thumbnail_url,
        String image_url,
        Integer width,
        Integer height,
        String display_sitename,
        String doc_url,
        String datetime
) {}
