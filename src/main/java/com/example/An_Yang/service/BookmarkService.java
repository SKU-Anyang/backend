package com.example.An_Yang.service;

import com.example.An_Yang.domain.Bookmark;
import com.example.An_Yang.domain.Idea;
import com.example.An_Yang.domain.User;
import com.example.An_Yang.repository.BookmarkRepository;
import com.example.An_Yang.repository.IdeaRepository;
import com.example.An_Yang.repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /** ★ 프론트 목록용 */
    @Transactional(readOnly = true)
    public List<BookmarkItem> list(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Bookmark> rows = bookmarkRepository.findAllByUser(user);
        return rows.stream().map(b -> {
            Idea i = b.getIdea();
            return BookmarkItem.builder()
                    .ideaId(i.getId())
                    .title(nv(getSafeTitle(i)))
                    .summary(nv(getSafeSummary(i)))
                    .build();
        }).toList();
    }

    /** ★ 큐레이션용 간단 요약 리스트 */
    @Transactional(readOnly = true)
    public List<String> listSummaries(String userId) {
        return list(userId).stream()
                .map(it -> it.title + " — " + it.summary)
                .toList();
    }

    private static String getSafeTitle(Idea i) {
        try {
            var m = i.getClass().getMethod("getTitle");
            Object v = m.invoke(i);
            return v == null ? null : v.toString();
        } catch (Exception ignore) { return null; }
    }

    private static String getSafeSummary(Idea i) {
        // 엔티티에 summary가 없다면 title이나 content로 대체
        try {
            var m = i.getClass().getMethod("getSummary");
            Object v = m.invoke(i);
            if (v != null) return v.toString();
        } catch (Exception ignore) { }
        try {
            var m = i.getClass().getMethod("getContent");
            Object v = m.invoke(i);
            if (v != null) return v.toString();
        } catch (Exception ignore) { }
        return getSafeTitle(i);
    }

    private static String nv(String s) { return s == null ? "-" : s; }

    @Value
    @Builder
    public static class BookmarkItem {
        Long ideaId;
        String title;
        String summary;
    }
}
