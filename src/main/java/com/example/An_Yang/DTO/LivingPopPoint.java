package com.example.An_Yang.DTO;

import lombok.*;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LivingPopPoint {
    private String time;        // "06","09","12","15","18","21" 등
    private double total;       // 전체 생활인구
    private Double target;      // 타깃(예: 20대) 인구 (옵션)
}

