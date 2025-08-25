package com.example.An_Yang.DTO;

public record FtcResponse(
        String resultCode,
        String resultMsg,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount,
        FtcItems items
) {}
