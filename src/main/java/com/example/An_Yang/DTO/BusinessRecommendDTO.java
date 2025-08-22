package com.example.An_Yang.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRecommendDTO {

    // 요청 DTO
    public static class Request {
        private String type; // 사업 종류 (예: 카페)
        private String location; // 선호 지역 (선택사항)
        private Integer budget; // 예산 (선택사항)
        private String experience; // 경험 (선택사항)
        private String preferences; // 기타 선호사항 (선택사항)
    }

    // 응답 DTO
    public static class Response {
        private Long id;
        private String type;
        private String location;
        private String franchise;
        private String analysis;
        private Integer investment;
        private String risk;
        private LocalDateTime created;
        private String preferences;
    }

    // 간단한 응답 DTO (GPT 응답용)
    public static class SimpleResponse {
        private String type;
        private String location;
        private String franchise;
        private String analysis;
    }
}
