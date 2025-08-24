package com.example.An_Yang.service;

import com.example.An_Yang.DTO.AgeShare;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.An_Yang.service.CsvUtils.*;

@Service
@RequiredArgsConstructor
public class MoisResidentService {

    private static final Logger log = LoggerFactory.getLogger(MoisResidentService.class);

    @Value("${data.csv.moisAgeShare}")
    private String csvPath;

    // "12세남자", "24세여자" 같은 헤더에서 숫자만 뽑기
    private static final Pattern AGE_NUM_PATTERN = Pattern.compile("(\\d+)");

    public Mono<List<AgeShare>> ageShareByDong(String yyyymm, String admCd) {
        final String targetYm = left(digits(yyyymm), 6);
        final String code10   = left(digits(admCd), 10);
        final String code8    = left(code10, 8);

        // 실제 파일 헤더에 맞춰 후보 지정
        String[] COL_YM   = {"기준연월","STD_MT","STDYM","STD_YYMM","기준_연월"};
        String[] COL_CODE = {"행정기관코드","ADM_CD","ADMD_CD","ADSTRD_CD","행정동코드","법정동코드"};

        // 10대 단위 버킷
        Map<String,Integer> bucket = new LinkedHashMap<>(Map.of(
                "10대", 0, "20대", 0, "30대", 0, "40대", 0, "50대", 0, "60대", 0, "70대+", 0
        ));

        boolean[] logged = {false};

        for (var rec : read(csvPath)) {
            Map<String,String> m = row(rec);
            if (!logged[0]) { log.info("[MOIS-CSV] headers={}", m.keySet()); logged[0] = true; }

            // 연월 매칭
            String ym = left(digits(get(m, COL_YM)), 6);
            if (!targetYm.equals(ym)) continue;

            // 코드 매칭(10자리 or 8자리)
            String cRaw = digits(get(m, COL_CODE));
            String c10  = left(cRaw, 10);
            String c8   = left(cRaw, 8);
            boolean match = (!code10.isEmpty() && code10.equals(c10))
                    || (!code8.isEmpty()  && code8.equals(c8));
            if (!match) continue;

            // 모든 컬럼 순회하며 "xx세남자/여자" 형태만 합산
            for (var e : m.entrySet()) {
                String k = e.getKey();       // 예: "24세남자"
                String v = e.getValue();

                // 남자/여자 총계/계, 이름/지역 등은 패스
                String keyLower = k.toLowerCase(Locale.ROOT);
                if (keyLower.equals("남자") || keyLower.equals("여자") || keyLower.equals("계")) continue;
                if (!keyLower.contains("세")) continue; // 연령 아닌 컬럼 스킵

                Integer age = extractAge(k);
                if (age == null) continue;

                String b = toDecadeBucket(age);
                int val = toI(v);
                bucket.merge(b, val, Integer::sum);
            }
        }

        int total = bucket.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return Mono.just(List.of());

        List<AgeShare> out = bucket.entrySet().stream()
                .map(en -> {
                    double pct = en.getValue() * 100.0 / total;
                    double rounded = Math.round(pct * 10.0) / 10.0;
                    return new AgeShare(en.getKey(), en.getValue(), rounded);
                })
                .collect(Collectors.toList());

        log.info("[MOIS-CSV] ym={} adm(8/10)={}/{} -> {} buckets (total={})",
                targetYm, code8, code10, out.size(), total);
        return Mono.just(out);
    }

    private static Integer extractAge(String header){
        Matcher m = AGE_NUM_PATTERN.matcher(header);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return null;
    }

    private static String toDecadeBucket(int age){
        if (age < 10)      return "10대";   // 10대 미만은 비율 작아 보통 10대에 포함 처리
        else if (age < 20) return "10대";
        else if (age < 30) return "20대";
        else if (age < 40) return "30대";
        else if (age < 50) return "40대";
        else if (age < 60) return "50대";
        else if (age < 70) return "60대";
        else               return "70대+";
    }
}
