package com.example.An_Yang.repository;
import com.example.An_Yang.domain.BusinessRecommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusinessRecommendRepository extends JpaRepository<BusinessRecommend, Long> {

    // 사업 종류별 추천 조회
    List<BusinessRecommend> findByTypeOrderByCreatedDesc(String type);

    // 최근 추천 조회
    List<BusinessRecommend> findTop10ByOrderByCreatedDesc();

    // 특정 기간 내 추천 조회
    List<BusinessRecommend> findByCreatedBetweenOrderByCreatedDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    // 리스크 레벨별 조회
    List<BusinessRecommend> findByRiskOrderByCreatedDesc(String risk);

    // 예산 범위별 조회
    @Query("SELECT b FROM BusinessRecommend b WHERE b.investment BETWEEN :minBudget AND :maxBudget ORDER BY b.created DESC")
    List<BusinessRecommend> findByInvestmentBetween(
            @Param("minBudget") Integer minBudget,
            @Param("maxBudget") Integer maxBudget);

    // 위치별 조회
    List<BusinessRecommend> findByLocationContainingOrderByCreatedDesc(String location);

    // 사업 종류와 위치로 조회
    List<BusinessRecommend> findByTypeAndLocationContainingOrderByCreatedDesc(
            String type, String location);

    // 사용자 선호도가 포함된 추천 조회
    @Query("SELECT b FROM BusinessRecommend b WHERE b.preferences LIKE %:preference% ORDER BY b.created DESC")
    List<BusinessRecommend> findByPreferencesContaining(@Param("preference") String preference);
}

