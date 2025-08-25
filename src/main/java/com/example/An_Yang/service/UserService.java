package com.example.An_Yang.service;

import com.example.An_Yang.DTO.LoginRequest;
import com.example.An_Yang.DTO.RegisterRequest;
import com.example.An_Yang.domain.User;
import com.example.An_Yang.repository.UserRepository;
import com.example.An_Yang.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public User registerSimple(RegisterRequest req) {
        if (!req.getPassword().equals(req.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 서로 다릅니다.");
        }
        if (userRepository.existsByUserId(req.getUserId())) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .userId(req.getUserId())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .region(req.getRegion())
                .interest(req.getInterest())
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest req) {
        User user = userRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }

        // 🔽 JwtUtil의 발급 메서드명에 맞게 수정
        String accessToken = jwtUtil.generateAccessToken(user.getUserId());

        return new LoginResult(user, accessToken);
    }

    public record LoginResult(User user, String accessToken) {}
}
