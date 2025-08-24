package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.BusinessRecommendDTO;
import com.example.An_Yang.service.BusinessRecommendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class BusinessRecommendController {

    private final BusinessRecommendService businessRecommendService;

    public BusinessRecommendController(BusinessRecommendService businessRecommendService) {
        this.businessRecommendService = businessRecommendService;
    }

    /**
     * 새로운 창업 추천 생성
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRecommendation(@RequestBody BusinessRecommendDTO.Request request) {
        try {
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "사업 종류를 입력해주세요."));
            }

            BusinessRecommendDTO.Response response = businessRecommendService.createRecommendation(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "창업 추천 생성 중 오류가 발생했습니다."));
        }
    }

    /**
     * ID로 추천 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecommendationById(@PathVariable Long id) {
        try {
            return businessRecommendService.getRecommendationById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 사업 종류별 추천 조회
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getRecommendationsByType(@PathVariable String type) {
        try {
            List<BusinessRecommendDTO.Response> recommendations =
                    businessRecommendService.getRecommendationsByType(type);
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "사업 종류별 추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 최근 추천 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentRecommendations() {
        try {
            List<BusinessRecommendDTO.Response> recommendations =
                    businessRecommendService.getRecentRecommendations();
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "최근 추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 기간별 추천 조회
     */
    @GetMapping("/period")
    public ResponseEntity<?> getRecommendationsByPeriod(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);

            List<BusinessRecommendDTO.Response> recommendations =
                    businessRecommendService.getRecommendationsByPeriod(start, end);
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "기간별 추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 리스크 레벨별 추천 조회
     */
    @GetMapping("/risk/{risk}")
    public ResponseEntity<?> getRecommendationsByRisk(@PathVariable String risk) {
        try {
            List<BusinessRecommendDTO.Response> recommendations =
                    businessRecommendService.getRecommendationsByRisk(risk);
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "리스크 레벨별 추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 예산 범위별 추천 조회
     */
    @GetMapping("/budget-range")
    public ResponseEntity<?> getRecommendationsByBudgetRange(
            @RequestParam Integer minBudget,
            @RequestParam Integer maxBudget) {
        try {
            List<BusinessRecommendDTO.Response> recommendations =
                    businessRecommendService.getRecommendationsByBudgetRange(minBudget, maxBudget);
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "예산 범위별 추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 위치별 추천 조회
     */
    @GetMapping("/location/{location}")
    public ResponseEntity<?> getRecommendationsByLocation(@PathVariable String location) {
        try {
            List<BusinessRecommendDTO.Response> recommendations =
                    businessRecommendService.getRecommendationsByLocation(location);
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "위치별 추천 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 추천 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecommendation(@PathVariable Long id) {
        try {
            businessRecommendService.deleteRecommendation(id);
            return ResponseEntity.ok(Map.of("message", "추천이 성공적으로 삭제되었습니다."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "추천 삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 서비스 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "message", "창업 추천 서비스가 정상 작동 중입니다."
        ));
    }
}
