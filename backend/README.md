# Backend - CoreBridge MSA

Spring Boot 기반 마이크로서비스 아키텍처

## 프로젝트 구조

```
backend/
├── settings.gradle          # 멀티모듈 설정
├── build.gradle             # 루트 빌드 설정
│
├── common/                  # 공통 모듈
│   ├── audit/              # 감사 로그
│   ├── domain/             # BaseTimeEntity
│   ├── response/           # BaseResponse
│   ├── exception/          # BaseException
│   └── snowflake/          # 분산 ID 생성기
│
├── infra/                   # 인프라 모듈 (Redis, Kafka 등)
│
└── service/                 # 서비스 모듈
    ├── user/               # 사용자 관리 (8001)
    ├── jobposting/         # 채용공고 (8002)
    ├── application/        # 지원서 (8003)
    ├── process/            # 채용 프로세스 (8004)
    ├── schedule/           # 면접 일정 (8005)
    ├── notification/       # 알림 (8006)
    ├── resume/             # 이력서 (8007)
    ├── jobposting-like/    # 좋아요 (8008)
    ├── jobposting-view/    # 조회수 (8009)
    ├── jobposting-read/    # 상세 조회 (8010)
    ├── jobposting-hot/     # 인기 공고 (8011)
    ├── jobposting-comment/ # 댓글 (8012)
    └── admin-audit/        # 감사 로그 (8013)
```

## 실행

```bash
# 전체 빌드
./gradlew build

# 개별 서비스 실행
./gradlew :service:user:bootRun

# 테스트
./gradlew test
```

## 기술 스택

- Java 21
- Spring Boot 3.5.9
- Spring Data JPA
- Spring Security
- PostgreSQL 18
- Gradle 8.x
