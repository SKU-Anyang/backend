package com.example.An_Yang.DTO;

import jakarta.validation.constraints.*;
import java.util.List;

public record BizDesignRequest(
        @NotBlank String industry,
        @NotBlank String region,
        @NotNull @Size(min = 1) List<String> target,
        @Positive long budgetKrw,
        @Min(0) int staff,
        @Positive double areaPyeong,
        String notes
) {}
