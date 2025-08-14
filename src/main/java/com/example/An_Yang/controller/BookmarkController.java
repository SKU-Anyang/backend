package com.example.An_Yang.controller;

import com.example.An_Yang.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /** 찜 등록: POST /api/bookmarks/{ideaId}  (토큰 필요) */
    @PostMapping("/{ideaId}")
    public ResponseEntity<?> add(@PathVariable Long ideaId, Authentication auth) {
        String userId = (String) auth.getPrincipal();  // JwtAuthFilter에서 userId를 principal로 넣었음
        boolean created = bookmarkService.add(userId, ideaId);
        if (created) return ResponseEntity.status(201).build(); // 201 Created
        return ResponseEntity.ok().build(); // 이미 있으면 200
    }

    /** 찜 취소: DELETE /api/bookmarks/{ideaId}  (토큰 필요) */
    @DeleteMapping("/{ideaId}")
    public ResponseEntity<?> remove(@PathVariable Long ideaId, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        bookmarkService.remove(userId, ideaId);
        return ResponseEntity.noContent().build(); // 204 No Content (idempotent)
    }
}
