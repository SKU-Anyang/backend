package com.example.An_Yang.controller;

import com.example.An_Yang.entity.ChatHistory;
import com.example.An_Yang.service.GptService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final GptService gptService;

    public ChatController(GptService gptService) {
        this.gptService = gptService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "질문을 입력해주세요."));
            }

            String response = gptService.getGptResponse(question);
            return ResponseEntity.ok(Map.of("answer", response));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatHistory>> getChatHistory() {
        try {
            List<ChatHistory> history = gptService.getChatHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "healthy", "message", "사장님 도우미 서비스가 정상 작동 중입니다."));
    }

    // 폐업 리스크 분석
    @PostMapping("/risk-analysis")
    public ResponseEntity<Map<String, String>> analyzeRisk(@RequestBody Map<String, String> request) {
        try {
            String businessType = request.get("businessType");
            String location = request.get("location");
            String investmentAmount = request.get("investmentAmount");
            String experience = request.get("experience");

            if (businessType == null || businessType.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "사업 종류를 입력해주세요."));
            }

            String analysis = gptService.analyzeBusinessRisk(businessType, location, investmentAmount, experience);
            return ResponseEntity.ok(Map.of("analysis", analysis));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "리스크 분석 중 오류가 발생했습니다."));
        }
    }

    // 창업 추천
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, String>> recommendBusiness(@RequestBody Map<String, String> request) {
        try {
            String businessType = request.get("businessType");
            String preferredLocation = request.get("preferredLocation");
            String budget = request.get("budget");

            if (businessType == null || businessType.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "원하는 사업 종류를 입력해주세요."));
            }

            String recommendation = gptService.recommendBusiness(businessType, preferredLocation, budget);
            return ResponseEntity.ok(Map.of("recommendation", recommendation));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "창업 추천 중 오류가 발생했습니다."));
        }
    }
}

