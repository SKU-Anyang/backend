package com.example.An_Yang.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 가게 + 대표이미지(첫 장) + 이미지 목록 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KakaoPlaceWithImage {
    private KakaoPlace place;
    private String imageUrl;         // 대표 이미지(첫 장)
    private List<String> imageUrls;  // 썸네일/원본 URL 리스트
}
