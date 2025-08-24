package com.example.An_Yang.DTO;

import lombok.*;

@Getter
@Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgeShare {
    private String bucket;  // "10대","20대",...
    private int count;
    private double ratio;   // %
}