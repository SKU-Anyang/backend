# 🏙️STARTIN — 안양 소상공인 창업 통계 서비스

> 멋쟁이사자처럼 해커톤 | 공공데이터 기반 소상공인 창업 통계 조회 백엔드 서버

<br>

## 📌 프로젝트 소개

**STARTIN**은 멋쟁이사자처럼 해커톤에서 개발한 프로젝트로,  
공공데이터 포털의 소상공인 창업통계 API를 활용하여  
안양 지역 업종별 창업·폐업 현황을 조회하고 분석할 수 있는 RESTful 백엔드 서버입니다.

- 공공데이터 포털 Open API 연동 (소상공인 창업통계)
- JWT 기반 사용자 인증 및 권한 관리
- Swagger UI를 통한 API 명세 제공

<br>

## 🛠 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![WebFlux](https://img.shields.io/badge/Spring_WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

### Database
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

### Tools & Docs
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Lombok](https://img.shields.io/badge/Lombok-BC4521?style=for-the-badge&logo=lombok&logoColor=white)

<br>

## 📁 프로젝트 구조

```
backend
├── src
│   └── main
│       ├── java/com/example
│       │   ├── domain
│       │   │   ├── auth          # 로그인 / 회원가입
│       │   │   ├── user          # 사용자 관리
│       │   │   └── stats         # 창업통계 조회
│       │   ├── global
│       │   │   ├── config        # Security, WebFlux 설정
│       │   │   ├── jwt           # JWT 발급 / 검증
│       │   │   └── exception     # 공통 예외 처리
│       │   └── AnyangApplication.java
│       └── resources
│           └── application.yml
├── build.gradle
└── README.md
```

<br>

## ⚙️ 실행 방법

### 1. 사전 요구사항

- Java 17 이상
- MySQL 8.0 이상
- Gradle 8.x

### 2. 데이터베이스 생성

```sql
CREATE DATABASE anyang CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. application.yml 설정

`src/main/resources/application.yml` 파일을 생성하고 아래 내용을 작성합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/anyang?useSSL=false&serverTimezone=Asia/Seoul
    username: YOUR_DB_USERNAME
    password: YOUR_DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: YOUR_JWT_SECRET_KEY
  expiration: 86400000  # 24시간 (ms)

public-data:
  service-key: YOUR_PUBLIC_DATA_SERVICE_KEY  # 공공데이터 포털 발급 키

server:
  port: 8080
```

### 4. 빌드 및 실행

```bash
# 레포지토리 클론
git clone https://github.com/SKU-Anyang/backend.git
cd backend

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

### 5. API 문서 확인 (Swagger UI)

서버 실행 후 브라우저에서 접속:

```
http://localhost:8080/swagger-ui/index.html
```

<br>

## 🔑 주요 API

| Method | URL | 설명 |
|:---:|:---|:---|
| `POST` | `/api/auth/signup` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 (JWT 발급) |
| `GET` | `/api/stats/opcl` | 업종별 창업통계 조회 |
| `GET` | `/api/stats/opcl/{indutyLclsCd}` | 특정 업종 창업통계 조회 |

> 인증이 필요한 API는 요청 헤더에 `Authorization: Bearer {JWT_TOKEN}` 을 포함해야 합니다.

<br>

## 📊 외부 API

본 프로젝트는 **공공데이터 포털**의 소상공인 창업통계 서비스를 사용합니다.

- 서비스명: 소상공인 창업·폐업 통계
- 제공기관: 소상공인시장진흥공단
- 포털: [https://www.data.go.kr](https://www.data.go.kr)
- 사용 엔드포인트: `FftcIndutyFrcsOpclStatsService/getOpclStats`

<br>

## 👥 팀원

| 이름 | 역할 | GitHub |
|:---:|:---:|:---:|
| 여준 | Backend | [@jjun012](https://github.com/jjun012) |
| 송은선 | Backend | [@somgnos](https://github.com/somgnos) |
| 유지수 | Frontend | [@jisu0811](https://github.com/jisu0811) |
| 김민규 | Frontkend | [@mingyu0126](https://github.com/mingyu0126) |

<br>

## 🦁 만든 곳

멋쟁이사자처럼 (LIKELION) 해커톤
