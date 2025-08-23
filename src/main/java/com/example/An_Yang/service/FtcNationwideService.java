package com.example.An_Yang.service;

import com.example.An_Yang.DTO.FtcItem;
import com.example.An_Yang.DTO.FtcItems;
import com.example.An_Yang.DTO.FtcResponse;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class FtcNationwideService {

    private final WebClient client;
    private final String serviceKey;
    private final String op;
    private final String resultTypeParam;   // 빈 값이면 붙이지 않음
    private final String resultTypeValue;
    private final XmlMapper xml = new XmlMapper();

    public FtcNationwideService(
            @Qualifier("ftcClient") WebClient client,
            @Value("${apis.ftc.serviceKey}") String serviceKey,
            @Value("${apis.ftc.op}") String op,
            @Value("${apis.ftc.resultTypeParam:}") String resultTypeParam,   // ← 기본값 빈 문자열
            @Value("${apis.ftc.resultTypeValue:}") String resultTypeValue   // ← 기본값 빈 문자열
    ) {
        this.client = client.mutate()
                .defaultHeader("Accept", "application/xml, application/json;q=0.8")
                .build();

        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.op = op == null ? "" : op.trim();
        this.resultTypeParam = resultTypeParam == null ? "" : resultTypeParam.trim();
        this.resultTypeValue = resultTypeValue == null ? "" : resultTypeValue.trim();
    }

    public Mono<List<FtcItem>> getOutfood(int yr, int pageNo, int rows) { return call(yr, "01", pageNo, rows); }
    public Mono<List<FtcItem>> getRetail (int yr, int pageNo, int rows) { return call(yr, "02", pageNo, rows); }
    public Mono<List<FtcItem>> getService(int yr, int pageNo, int rows) { return call(yr, "03", pageNo, rows); }

    public Mono<String> rawOutfood(int yr, int pageNo, int rows) { return rawCall(yr, "01", pageNo, rows); }

    private Mono<List<FtcItem>> call(int yr, String indutyLclsCd, int pageNo, int rows) {
        return rawCall(yr, indutyLclsCd, pageNo, rows)
                .flatMap(body -> Mono.fromCallable(() -> xml.readValue(body, FtcResponse.class)))
                .map(resp -> Optional.ofNullable(resp)
                        .map(FtcResponse::items)
                        .map(FtcItems::item)
                        .orElseGet(List::of))
                .onErrorMap(ex -> new RuntimeException("[FTC] XML 파싱 실패: " + ex.getMessage(), ex));
    }

    private Mono<String> rawCall(int yr, String indutyLclsCd, int pageNo, int rows) {
        int safePage = Math.max(1, pageNo);
        int safeRows = Math.max(1, rows);

        return client.get()
                .uri(uri -> {
                    var b = uri.path("/" + op)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("yr", yr)
                            .queryParam("indutyLclsCd", indutyLclsCd)
                            .queryParam("pageNo", safePage)
                            .queryParam("numOfRows", safeRows);
                    if (!resultTypeParam.isBlank() && !resultTypeValue.isBlank()) {
                        b.queryParam(resultTypeParam, resultTypeValue);
                    }
                    return b.build();
                })
                .retrieve()
                .onStatus(s -> s.isError(), resp -> resp.bodyToMono(String.class)
                        .map(body -> new RuntimeException("[FTC] " + resp.statusCode() + " - " + truncate(body, 1200))))
                .bodyToMono(String.class);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
