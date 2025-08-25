package com.example.An_Yang.service;

import com.example.An_Yang.DTO.CompetitorSummary;
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
public class SbizStoreService {

    private static final Logger log = LoggerFactory.getLogger(SbizStoreService.class);

    @Value("${data.csv.stores}")
    private String csvPath;

    public Mono<CompetitorSummary> countInRadius(double lat, double lng, int radiusM,
                                                 @Nullable List<String> catCodes) {

        // 열 이름 후보
        String[] LAT = {"lat","y","LAT","위도","LTTD"};
        String[] LNG = {"lng","x","LNG","경도","LONGTD"};
        String[] MCLS_CD = {"indsMclsCd","inds_mcls_cd","MCLS_CD"};
        String[] MCLS_NM = {"indsMclsNm","inds_mcls_nm","MCLS_NM","중분류명"};
        String[] SCLS_NM = {"indsSclsNm","inds_scls_nm","SCLS_NM","소분류명"};
        String[] CAT_GRP = {"category_group_code","cat_code"};

        // 반경(m)
        double radius = Math.max(1, radiusM);

        Map<String,Integer> byCat = new LinkedHashMap<>();
        int total = 0;

        for (var rec : read(csvPath)) {
            Map<String,String> m = row(rec);

            double y = toD(get(m, LAT));
            double x = toD(get(m, LNG));
            if (y == 0 || x == 0) continue;

            if (distanceMeter(lat, lng, y, x) > radius) continue;

            // 카테고리 필터(있을 때만)
            if (catCodes != null && !catCodes.isEmpty()) {
                String code = get(m, MCLS_CD);
                if (!code.isEmpty() && !catCodes.contains(code)) {
                    continue;
                }
            }

            String name =
                    nz(get(m, SCLS_NM),
                            nz(get(m, MCLS_NM),
                                    nz(get(m, CAT_GRP), "기타")));

            byCat.merge(name, 1, Integer::sum);
            total++;
        }

        CompetitorSummary out = CompetitorSummary.builder()
                .total(total)
                .byCategory(byCat.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue,
                                (a,b)->a, LinkedHashMap::new)))
                .build();

        log.info("[SBIZ-CSV] radius={}m total={} buckets={}", radiusM, total, out.getByCategory().size());
        return Mono.just(out);
    }

    private static String nz(String s, String def){ return (s==null || s.isBlank())? def : s; }

    // 하버사인 거리(m)
    private static double distanceMeter(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }
}
