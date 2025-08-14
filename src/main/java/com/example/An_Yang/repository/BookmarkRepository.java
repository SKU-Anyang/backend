package com.example.An_Yang.repository;

import com.example.An_Yang.domain.Bookmark;
import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUserAndIdea(User user, Idea idea);
    Optional<Bookmark> findByUserAndIdea(User user, Idea idea);
    long countByIdea(Idea idea); // (선택) 아이디어 찜 수
}
