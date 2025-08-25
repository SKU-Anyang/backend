package com.example.An_Yang.service;

import com.example.An_Yang.DTO.LivingPopPoint;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.An_Yang.service.CsvUtils.*;

@Service
@RequiredArgsConstructor
public class SeoulLivingPopService {

    private static final Logger log = LoggerFactory.getLogger(SeoulLivingPopService.class);

    @Value("${data.csv.seoulLivingPop}")
    private String csvPath;

    public Mono<List<LivingPopPoint>> hourlyByDong(String yyyymmdd, String dongCode, @Nullable String tmzonPdSe) {
        final String targetDate = left(digits(yyyymmdd), 8);  // 2025-07-01 -> 20250701
        final String code10 = left(digits(dongCode), 10);
        final String code8  = left(code10, 8);

        // ★ 실제 파일 헤더 반영
        String[] COL_DATE = {"STD_YMD","STDR_YYMMDD","STDR_DATE","BASE_DATE","기준일자","기준일ID"};
        String[] COL_DONG = {"ADSTRD_CD","ADM_CD","ADMD_CD","법정동코드","행정동코드","ADSTRD_CODE_SE","ADSTRD_CODE"};
        String[] COL_TIME = {"TMZON_PD_SE","TMZON_PD","HOUR_CD","TMZON","TIME","시간대","시간대구분"};
        String[] COL_TOT  = {"TOT_LVPOP_CO","TOT_POP","TOTAL","합계","총생활인구수"};

        Map<String, Double> byTime = new LinkedHashMap<>();
        boolean[] logged = {false};

        for (var rec : read(csvPath)) {
            Map<String,String> m = row(rec);
            if (!logged[0]) { log.info("[Seoul-CSV] headers={}", m.keySet()); logged[0]=true; }

            String dNorm = left(digits(get(m, COL_DATE)), 8);
            if (!targetDate.equals(dNorm)) continue;

            String cRaw = digits(get(m, COL_DONG));
            String c10  = left(cRaw,10);
            String c8   = left(cRaw, 8);
            boolean codeMatch =
                    (!code10.isEmpty() && code10.equals(c10)) ||
                            (!code8.isEmpty()  && code8.equals(c8));
            if (!codeMatch) continue;

            String hh = get(m, COL_TIME);                 // 값 예: "06-09" 또는 "06"
            if (tmzonPdSe != null && !tmzonPdSe.isBlank() && !tmzonPdSe.equals(hh)) continue;

            double tot = toD(get(m, COL_TOT));
            byTime.merge(hh, tot, Double::sum);
        }

        List<LivingPopPoint> out = byTime.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> LivingPopPoint.builder()
                        .time(mapTimeLabel(e.getKey()))
                        .total(Math.rint(e.getValue()))
                        .build())
                .collect(Collectors.toList());

        log.info("[Seoul-CSV] date={} adm(8/10)={}/{} -> {} rows", targetDate, code8, code10, out.size());
        return Mono.just(out);
    }

    private static String mapTimeLabel(String code){
        if (code != null && code.matches("\\d{2}")) {
            int hh = Integer.parseInt(code);
            return String.format("%02d-%02d", hh, (hh+3)%24);
        }
        return code == null ? "" : code;
    }
}
