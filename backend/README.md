# Backend - CoreBridge MSA

Spring Boot 기반 마이크로서비스 아키텍처

## 서비스 구성

| 서비스 | 포트 | 설명 |
|--------|------|------|
| user | 8001 | 사용자 관리 |
| jobposting | 8002 | 채용공고 |
| application | 8003 | 지원서 |
| process | 8004 | 채용 프로세스 (State Machine) |
| schedule | 8005 | 면접 일정 |
| notification | 8006 | 알림 |
| resume | 8007 | 이력서 |
| jobposting-like | 8008 | 좋아요 |
| jobposting-view | 8009 | 조회수 |
| jobposting-read | 8010 | 상세 조회 |
| jobposting-hot | 8011 | 인기 공고 |
| jobposting-comment | 8012 | 댓글 |
| admin-audit | 8013 | 감사 로그 |

## 실행

```bash
# 전체 서비스 실행
./gradlew bootRun --parallel

# 개별 서비스 실행
./gradlew :service:user:bootRun
./gradlew :service:process:bootRun
```

## 기술 스택

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL 18
- Gradle
