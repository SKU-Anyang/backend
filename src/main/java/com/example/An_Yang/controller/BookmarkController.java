package com.example.An_Yang.controller;

import com.example.An_Yang.service.BookmarkService;
import com.example.An_Yang.service.IdeaService;
import com.example.An_Yang.domain.Idea;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmarks") // /api 로 통일
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final IdeaService ideaService; // ★ 추가: 아이디어 없는 케이스에서 upsert 위해

    /** 1) (기존) 찜 등록: POST /api/bookmarks/{ideaId}  (토큰 필요)
     *    -> ideaId가 이미 있는 카드에 사용 */
    @PostMapping("/{ideaId}")
    public ResponseEntity<?> add(@PathVariable Long ideaId, Authentication auth) {
        String userId = (String) auth.getPrincipal();  // JwtAuthFilter에서 userId를 principal로 넣었음
        boolean created = bookmarkService.add(userId, ideaId);
        if (created) return ResponseEntity.status(201).build(); // 201 Created
        return ResponseEntity.ok().build(); // 이미 있으면 200
    }

    /** 1-β) ★ 아이디어 ID가 아직 없을 때: POST /api/bookmarks  (토큰 필요)
     *  - 바디로 title/industry/region(필수) + 기타 필드를 받아서
     *    1) Idea upsert -> 2) 북마크 등록 -> 3) ideaId 반환
     *  - 프론트는 /api/ai/idea/bookmark-and-suggest 대신 이걸 써도 됨(유사추천 필요 없을 때)
     */
    @PostMapping
    public ResponseEntity<AddByPayloadRes> addByPayload(@RequestBody AddByPayloadReq req, Authentication auth) {
        String userId = (String) auth.getPrincipal();

        // (A) ideaId가 오면 바로 등록
        if (req.ideaId != null) {
            boolean created = bookmarkService.add(userId, req.ideaId);
            return created
                    ? ResponseEntity.status(201).body(new AddByPayloadRes(req.ideaId, true))
                    : ResponseEntity.ok(new AddByPayloadRes(req.ideaId, false));
        }

        // (B) ideaId가 없으면 title/industry/region 필수
        if (isBlank(req.title) || isBlank(req.industry) || isBlank(req.region)) {
            return ResponseEntity.badRequest().build();
        }

        // 1) 아이디어 upsert (title+region+industry 기준)
        Idea idea = ideaService.upsertIdeaFromAi(
                userId,
                req.title,
                req.summary,
                req.industry,
                req.region,
                req.contentJson,
                req.closureYear,
                req.closureRate
        );

        // 2) 북마크 등록 (idempotent)
        boolean created = bookmarkService.add(userId, idea.getId());

        // 3) 결과 반환
        return created
                ? ResponseEntity.status(201).body(new AddByPayloadRes(idea.getId(), true))
                : ResponseEntity.ok(new AddByPayloadRes(idea.getId(), false));
    }

    /** 2) 찜 취소: DELETE /api/bookmarks/{ideaId}  (토큰 필요) */
    @DeleteMapping("/{ideaId}")
    public ResponseEntity<?> remove(@PathVariable Long ideaId, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        bookmarkService.remove(userId, ideaId);
        return ResponseEntity.noContent().build(); // 204 No Content (idempotent)
    }

    /** 3) 찜 목록: GET /api/bookmarks  (토큰 필요) */
    @GetMapping
    public ResponseEntity<List<BookmarkService.BookmarkItem>> list(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.ok(bookmarkService.list(userId));
    }

    /* ---------- DTO & utils ---------- */

    // 요청: ideaId가 있으면 그걸 쓰고, 없으면 아래 필드로 upsert
    public record AddByPayloadReq(
            Long ideaId,
            String title,
            String summary,
            String industry,
            String region,
            String contentJson,
            Integer closureYear,
            Double closureRate
    ) {}

    // 응답: 생성/기존 여부와 최종 ideaId
    public record AddByPayloadRes(
            Long ideaId,
            boolean created // true=새로 북마크 생성, false=이미 있었음
    ) {}

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
