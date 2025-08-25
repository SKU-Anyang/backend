package com.example.An_Yang.repository;

import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IdeaRepository extends JpaRepository<Idea, Long> {

    List<Idea> findByCreatedBy(User user);

    /** ★ 원샷 upsert용: title + region + industry 기준 중복 방지 */
    Optional<Idea> findByTitleAndRegionAndIndustry(String title, String region, String industry);
}
