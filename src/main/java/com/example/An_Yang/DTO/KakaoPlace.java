package com.example.An_Yang.DTO;

public record KakaoPlace(
        String id, String place_name, String category_group_code, String category_name,
        String phone, String address_name, String road_address_name,
        String x, String y, String place_url
) {
    public String uniqueKey() {
        if (id != null && !id.isBlank()) return id;
        return (place_name + "|" + road_address_name).toLowerCase();
    }
}
