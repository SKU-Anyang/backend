package com.example.An_Yang.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivingPopPoint {
    private String time;     // "06-09" 같은 구간 라벨
    private double total;    // 전체 유동인구

    @JsonInclude(JsonInclude.Include.NON_NULL)

    private Double male;     // 남자 유동인구(옵션)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double female;   // 여자 유동인구(옵션)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double target;   // 타깃 연령(예: 20대) 유동인구(옵션)
}

