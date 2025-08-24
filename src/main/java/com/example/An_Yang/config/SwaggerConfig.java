package com.example.An_Yang.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /** 상단 전역 설명 + Bearer 인증 스키마(Authorize 버튼) */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("An-Yang API")
                        .version("v1")
                        .description("""
                                안양 상권 분석/창업 지원 백엔드 API

                                - 인증: 회원 가입/로그인(JWT)
                                - AI: 창업 아이디어 추천, 비즈니스 설계, 사장님 B2B 도우미, 큐레이션
                                - 진단: 시장성 정량분석(JSON), 폐업 리스크
                                - 카카오: 주변 유사 점포 검색, 단건 점포 분석
                                - 북마크: 아이디어 찜 등록/조회/삭제
                                - 추천 CRUD: 관리자/운영용 엔드포인트
                                """)
                        .contact(new Contact().name("Team An-Yang").email("ops@example.com"))
                        .license(new License().name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("Authorization")))
                // 태그(섹션) 설명 – 컨트롤러의 @Tag(name="...")와 이름이 같으면 병합되어 설명이 화면에 표시됩니다.
                .addTagsItem(new Tag().name("Auth").description("회원 가입 / 로그인, JWT 발급"))
                .addTagsItem(new Tag().name("AI").description("아이디어 추천, 비즈니스 설계, 사장님 B2B 도우미, 큐레이션"))
                .addTagsItem(new Tag().name("Diagnosis").description("시장성 정량 진단(JSON), 폐업 리스크"))
                .addTagsItem(new Tag().name("Kakao Local").description("카카오 장소 검색, 단일 점포 분석"))
                .addTagsItem(new Tag().name("Bookmarks").description("아이디어 찜(등록/삭제/목록)"))
                .addTagsItem(new Tag().name("Recommendations").description("추천 생성/조회/삭제"));
        // 전역 SecurityRequirement는 걸지 않음(공개 API 잠김 방지)
    }

    /** 그룹(상단 드롭다운) – 각 그룹 선택 시 상단 제목·설명을 오버라이드 */
    @Bean public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/auth/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("Auth API")
                        .version("v1")
                        .description("회원 가입 / 로그인 및 JWT 발급")))
                .build();
    }

    @Bean public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("ai")
                .pathsToMatch("/api/ai/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("AI API")
                        .version("v1")
                        .description("창업 아이디어 추천, 비즈니스 설계, 사장님 B2B 도우미, 사용자 큐레이션")))
                .build();
    }

    @Bean public GroupedOpenApi kakaoApi() {
        return GroupedOpenApi.builder()
                .group("kakao")
                .pathsToMatch("/api/kakao/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("Kakao Local API")
                        .version("v1")
                        .description("카카오 키워드 기반 주변 점포 검색 및 단건 점포 분석")))
                .build();
    }

    @Bean public GroupedOpenApi diagApi() {
        return GroupedOpenApi.builder()
                .group("diag")
                .pathsToMatch("/api/diag/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("Diagnosis API")
                        .version("v1")
                        .description("시장성 정량 계산(JSON)과 폐업 리스크 분석")))
                .build();
    }

    @Bean public GroupedOpenApi bookmarksApi() {
        return GroupedOpenApi.builder()
                .group("bookmarks")
                .pathsToMatch("/api/bookmarks/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("Bookmarks API")
                        .version("v1")
                        .description("아이디어 찜 등록/삭제/목록 조회 (JWT 필요)")))
                .build();
    }

    @Bean public GroupedOpenApi recommendationsApi() {
        return GroupedOpenApi.builder()
                .group("recommendations")
                .pathsToMatch("/api/recommendations/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("Recommendations API")
                        .version("v1")
                        .description("추천 생성/조회/삭제 등 CRUD")))
                .build();
    }
}
