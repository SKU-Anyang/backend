package com.example.An_Yang.service;

import com.example.An_Yang.domain.Bookmark;
import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import com.example.An_Yang.repository.BookmarkRepository;
import com.example.An_Yang.repository.IdeaRepository;
import com.example.An_Yang.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final UserRepository userRepository;
    private final IdeaRepository ideaRepository;
    private final BookmarkRepository bookmarkRepository;

    /** 찜 등록 (idempotent) — 이미 있으면 그대로 OK */
    @Transactional
    public boolean add(String userId, Long ideaId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new IllegalArgumentException("아이디어가 존재하지 않습니다."));

        if (bookmarkRepository.existsByUserAndIdea(user, idea)) {
            return false; // 이미 있음
        }
        bookmarkRepository.save(Bookmark.builder().user(user).idea(idea).build());
        return true; // 새로 생성됨
    }

    /** 찜 취소 (idempotent) — 없으면 조용히 넘어감 */
    @Transactional
    public void remove(String userId, Long ideaId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new IllegalArgumentException("아이디어가 존재하지 않습니다."));

        bookmarkRepository.findByUserAndIdea(user, idea)
                .ifPresent(bookmarkRepository::delete);
    }
}
