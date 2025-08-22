package com.example.An_Yang.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "business_recommendations")
public class BusinessRecommend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // 사업 종류 (예: 카페, 음식점, 편의점 등)

    @Column(nullable = false)
    private String location; // 추천 위치

    @Column(nullable = false)
    private String franchise; // 프랜차이즈 추천

    @Column(columnDefinition = "TEXT")
    private String analysis; // 상세 분석 내용

    @Column(nullable = false)
    private LocalDateTime created; // 생성 시간

    @Column
    private String preferences; // 사용자 선호도 (예산, 선호 지역 등)

    @Column
    private Integer investment; // 예상 투자금액

    @Column
    private String risk; // 리스크 레벨 (낮음, 보통, 높음)

    public BusinessRecommend(String type, String location,
                             String franchise, String analysis,
                             String preferences, Integer investment,
                             String risk) {
        this.type = type;
        this.location = location;
        this.franchise = franchise;
        this.analysis = analysis;
        this.preferences = preferences;
        this.investment = investment;
        this.risk = risk;
        this.created = LocalDateTime.now();
    }
}
