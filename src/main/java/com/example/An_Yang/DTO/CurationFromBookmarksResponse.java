package com.example.An_Yang.DTO;


import lombok.*;
import java.util.*;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CurationFromBookmarksResponse {
    private List<String> likedIdeaSummaries; // 찜에 저장된 아이디어 요약
    private String aiCuration; // 새 추천 결과
}