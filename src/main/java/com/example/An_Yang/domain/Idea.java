// domain/Idea.java
package com.example.An_Yang.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "idea")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Idea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 생성자
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private String title;

    @Column(length = 1000)
    private String summary;

    @Lob
    private String contentJson; // AI 원본 JSON(선택)

    private String industry;
    private String region;

    private Integer closureYear;
    private Double closureRate;

    private LocalDateTime createdAt;
}
