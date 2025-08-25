package com.example.An_Yang.DTO;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KakaoSummaryResponse {
    private int total;
    private List<Item> items;
    private String aiSummary;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private String name;
        private String category;
        private String address;
        private double distanceMeters;
    }
}