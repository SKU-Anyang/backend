package com.example.An_Yang.DTO;// package DTO;
import lombok.Data;

@Data
public class RegisterRequest {
    private String userId;           // ✅ 아이디
    private String email;            // 이메일
    private String password;         // 비번
    private String passwordConfirm;  // 비번 재확인
    private String nickname;
    private String region;
    private String interest;
}
