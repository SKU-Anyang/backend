package com.example.An_Yang.repository;

import com.example.An_Yang.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // 1. 가장 최근 대화 순으로 조회 (Pageable로 limit 처리)
    @Query("SELECT c FROM ChatHistory c ORDER BY c.askedAt DESC")
    List<ChatHistory> findAllOrderByAskedAtDesc(Pageable pageable);

    // 2. 특정 기간 안의 대화 조회
    List<ChatHistory> findByAskedAtBetweenOrderByAskedAtDesc(LocalDateTime start, LocalDateTime end);

    // 3. 질문 내용에 특정 키워드가 포함된 대화 조회
    @Query("SELECT c FROM ChatHistory c WHERE LOWER(c.question) LIKE LOWER(concat('%', :keyword, '%'))")
    List<ChatHistory> findByQuestionKeyword(String keyword);

    // 4. 답변에 특정 단어가 포함된 대화 조회
    @Query("SELECT c FROM ChatHistory c WHERE LOWER(c.answer) LIKE LOWER(concat('%', :keyword, '%'))")
    List<ChatHistory> findByAnswer(String keyword);

    // 5. 특정 키워드 필터 후 최신순 조회 (Pageable로 limit 처리)
    @Query("SELECT c FROM ChatHistory c WHERE LOWER(c.question) LIKE LOWER(concat('%', :keyword, '%')) ORDER BY c.askedAt DESC")
    List<ChatHistory> searchQuestionKeyword(String keyword, Pageable pageable);
}
