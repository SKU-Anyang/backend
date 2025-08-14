package com.example.An_Yang.controller;

import com.example.An_Yang.DTO.LoginRequest;
import com.example.An_Yang.DTO.RegisterRequest;
import com.example.An_Yang.domain.User;
import com.example.An_Yang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody RegisterRequest req) {
        User u = userService.registerSimple(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SimpleRes(u.getUserId(), u.getEmail(), u.getNickname()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            var r = userService.login(req);
            var u = r.user();
            return ResponseEntity.ok(new LoginRes(u.getUserId(), u.getEmail(), u.getNickname(), r.accessToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorRes("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    private record SimpleRes(String userId, String email, String nickname) {}
    private record LoginRes(String userId, String email, String nickname, String accessToken) {}
    private record ErrorRes(String code, String message) {}
}
