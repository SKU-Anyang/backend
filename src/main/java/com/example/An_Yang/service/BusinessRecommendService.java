package com.example.An_Yang.service;

import com.example.An_Yang.domain.BusinessRecommend;
import com.example.An_Yang.dto.BusinessRecommendDTO;
import com.example.An_Yang.repository.BusinessRecommendRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessRecommendService {

    private final BusinessRecommendRepository businessRecommendRepository;
    private final GptService gptService;

    public BusinessRecommendService(BusinessRecommendRepository businessRecommendRepository,
                                    GptService gptService) {
        this.businessRecommendRepository = businessRecommendRepository;
        this.gptService = gptService;
    }

    /**
     * 새로운 창업 추천 생성
     */
    public BusinessRecommendDTO.Response createRecommendation(BusinessRecommendDTO.Request request) {
        // GPT를 통해 추천 분석 수행
        String gptResponse = gptService.getBusinessRecommendation(
                request.getType(),
                request.getLocation(),
                request.getBudget(),
                request.getExperience(),
                request.getPreferences()
        );

        // GPT 응답을 파싱하여 추천 정보 추출
        BusinessRecommend recommendation = parseGptResponse(request, gptResponse);

        // 데이터베이스에 저장
        BusinessRecommend savedRecommendation = businessRecommendRepository.save(recommendation);

        return convertToResponseDTO(savedRecommendation);
    }

    /**
     * GPT 응답을 파싱하여 BusinessRecommend 객체 생성
     */
    private BusinessRecommend parseGptResponse(BusinessRecommendDTO.Request request, String gptResponse) {
        // GPT 응답에서 정보 추출 (실제 구현에서는 더 정교한 파싱 필요)
        String[] parts = gptResponse.split("\\|");

        String location = parts.length > 0 ? parts[0].trim() : "위치 정보 없음";
        String franchise = parts.length > 1 ? parts[1].trim() : "프랜차이즈 정보 없음";
        String analysis = parts.length > 2 ? parts[2].trim() : gptResponse;
        Integer investment = parts.length > 3 ? parseInvestment(parts[3]) : null;
        String risk = parts.length > 4 ? parts[4].trim() : "보통";

        String preferences = buildUserPreferences(request);

        return new BusinessRecommend(
                request.getType(),
                location,
                franchise,
                analysis,
                preferences,
                investment,
                risk
        );
    }

    /**
     * 사용자 선호도 문자열 생성
     */
    private String buildUserPreferences(BusinessRecommendDTO.Request request) {
        StringBuilder preferences = new StringBuilder();
        if (request.getLocation() != null) {
            preferences.append("선호지역:").append(request.getLocation()).append(" ");
        }
        if (request.getBudget() != null) {
            preferences.append("예산:").append(request.getBudget()).append("만원 ");
        }
        if (request.getExperience() != null) {
            preferences.append("경험:").append(request.getExperience()).append(" ");
        }
        if (request.getPreferences() != null) {
            preferences.append("기타:").append(request.getPreferences());
        }
        return preferences.toString().trim();
    }

    /**
     * 투자금액 파싱
     */
    private Integer parseInvestment(String investmentStr) {
        try {
            return Integer.parseInt(investmentStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * ID로 추천 조회
     */
    @Transactional(readOnly = true)
    public Optional<BusinessRecommendDTO.Response> getRecommendationById(Long id) {
        return businessRecommendRepository.findById(id)
                .map(this::convertToResponseDTO);
    }

    /**
     * 사업 종류별 추천 조회
     */
    @Transactional(readOnly = true)
    public List<BusinessRecommendDTO.Response> getRecommendationsByType(String type) {
        return businessRecommendRepository.findByTypeOrderByCreatedDesc(type)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 최근 추천 조회
     */
    @Transactional(readOnly = true)
    public List<BusinessRecommendDTO.Response> getRecentRecommendations() {
        return businessRecommendRepository.findTop10ByOrderByCreatedDesc()
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 기간별 추천 조회
     */
    @Transactional(readOnly = true)
    public List<BusinessRecommendDTO.Response> getRecommendationsByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return businessRecommendRepository.findByCreatedBetweenOrderByCreatedDesc(startDate, endDate)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 리스크 레벨별 추천 조회
     */
    @Transactional(readOnly = true)
    public List<BusinessRecommendDTO.Response> getRecommendationsByRisk(String risk) {
        return businessRecommendRepository.findByRiskOrderByCreatedDesc(risk)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 예산 범위별 추천 조회
     */
    @Transactional(readOnly = true)
    public List<BusinessRecommendDTO.Response> getRecommendationsByBudgetRange(Integer minBudget, Integer maxBudget) {
        return businessRecommendRepository.findByInvestmentBetween(minBudget, maxBudget)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 위치별 추천 조회
     */
    @Transactional(readOnly = true)
    public List<BusinessRecommendDTO.Response> getRecommendationsByLocation(String location) {
        return businessRecommendRepository.findByLocationContainingOrderByCreatedDesc(location)
                .stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 추천 삭제
     */
    public void deleteRecommendation(Long id) {
        businessRecommendRepository.deleteById(id);
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private BusinessRecommendDTO.Response convertToResponseDTO(BusinessRecommend entity) {
        BusinessRecommendDTO.Response response = new BusinessRecommendDTO.Response();
        response.setId(entity.getId());
        response.setType(entity.getType());
        response.setLocation(entity.getLocation());
        response.setFranchise(entity.getFranchise());
        response.setAnalysis(entity.getAnalysis());
        response.setInvestment(entity.getInvestment());
        response.setRisk(entity.getRisk());
        response.setCreated(entity.getCreated());
        response.setPreferences(entity.getPreferences());
        return response;
    }
}

