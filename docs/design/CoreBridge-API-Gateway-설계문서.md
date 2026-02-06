# CoreBridge API Gateway 설계 문서

## 1. 개요

### 목적
모든 클라이언트 요청의 단일 진입점으로서 JWT 인증 중앙화, 라우팅, CORS 처리를 담당한다.
각 서비스가 개별적으로 JWT 검증하는 대신 Gateway에서 한번만 검증하여 책임 분리.

### 구현 상태: ✅ 완료
### 기술: Spring Cloud Gateway (WebFlux 기반)

---

## 2. 아키텍처

```
                           ┌──────────────────────────┐
[Frontend]                 │    API Gateway (:8000)    │
[Vue.js :5173] ──────────→ │                          │
                           │  1. CORS 처리             │
                           │  2. JWT 검증 (3단계)      │
                           │  3. 헤더 주입              │
                           │     X-User-Id             │
                           │     X-User-Email          │
                           │     X-User-Role           │
                           │  4. 라우팅                 │
                           └──────────┬───────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
    [user :8001]          [jobposting :8002]        [apply :8009]
    [comment :8003]       [view :8004]             [resume :8008]
    [like :8005]          [hot :8006]              [schedule :8012]
    [read :8007]          [notification :8011]     [audit :8013]
```

---

## 3. JWT 인증 3단계

### 3-1. 완전 공개 (인증 스킵)
토큰 검사 자체를 하지 않는 경로.

| 경로 | 용도 |
|------|------|
| `/api/v1/users/signup` | 회원가입 |
| `/api/v1/users/login` | 로그인 |
| `/api/v1/users/refresh` | 토큰 갱신 |
| `/actuator/**` | 헬스체크 |

### 3-2. Optional 인증 (GET 공개)
토큰 있으면 인증 시도, 없으면 비인증으로 통과.
사용자 로그인 여부에 따라 다른 UI를 보여줄 때 사용.

| 경로 (GET만) | 용도 |
|------|------|
| `/api/v1/jobpostings/**` | 공고 조회 |
| `/api/v1/jobposting-read/**` | 통계 포함 조회 |
| `/api/v1/hot-jobpostings/**` | 인기 공고 |
| `/api/v1/comments/**` | 댓글 조회 |
| `/api/v1/jobposting-views/**` | 조회수 |
| `/api/v1/jobposting-likes/**` | 좋아요수 |

### 3-3. 인증 필수
나머지 모든 경로. 토큰 없으면 401 Unauthorized.

---

## 4. 토큰 처리 방식

### Cookie 기반 (HttpOnly)
```
클라이언트 → Cookie: accessToken=eyJhbG... → Gateway
```

**Authorization Header가 아닌 Cookie를 사용하는 이유:**
- HttpOnly Cookie는 XSS 공격으로부터 안전
- 브라우저가 자동으로 Cookie 전송 → 프론트엔드 코드 간소화
- CSRF는 SameSite + CORS로 방어

### 토큰 추출
```java
private String resolveToken(ServerHttpRequest request) {
    HttpCookie cookie = request.getCookies().getFirst("accessToken");
    return cookie != null ? cookie.getValue() : null;
}
```

### 검증 후 헤더 주입
```java
Claims claims = validateAndGetClaims(token);
ServerHttpRequest mutatedRequest = request.mutate()
    .header("X-User-Id", claims.getSubject())
    .header("X-User-Email", claims.get("email", String.class))
    .header("X-User-Role", claims.get("role", String.class))
    .build();
```

**Downstream 서비스**에서는 `X-User-Id` 헤더만 읽으면 됨 → JWT 라이브러리 불필요.

---

## 5. 라우팅 테이블

| Route ID | Path | Target | Port |
|----------|------|--------|------|
| user-service | `/api/v1/users/**` | user | 8001 |
| jobposting-service | `/api/v1/jobpostings/**` | jobposting | 8002 |
| jobposting-comment | `/api/v1/comments/**` | comment | 8003 |
| jobposting-view | `/api/v1/jobposting-views/**` | view | 8004 |
| jobposting-like | `/api/v1/jobposting-likes/**` | like | 8005 |
| jobposting-hot | `/api/v1/hot-jobpostings/**` | hot | 8006 |
| jobposting-read | `/api/v1/jobposting-read/**` | read | 8007 |
| resume-service | `/api/v1/resumes/**` | resume | 8008 |
| apply-service | `/api/v1/applies/**`, `/api/v1/processes/**`, `/api/v1/ai-matching/**` | apply | 8009 |
| notification-service | `/api/v1/notifications/**` | notification | 8011 |
| schedule-service | `/api/v1/schedules/**` | schedule | 8012 |
| admin-audit | `/api/v1/admin/**`, `/api/v1/audit/**` | audit | 8013 |

**라우팅 순서**: 더 구체적인 경로가 먼저 (read > jobposting)

---

## 6. CORS 설정

```java
config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);  // Cookie 전송 허용
```

**주의**: `AllowCredentials(true)` + `AllowedOrigins("*")` 조합은 브라우저에서 거부됨.
반드시 구체적인 Origin을 지정해야 함.

---

## 7. Gateway ↔ 서비스 아키텍처 차이

| 구분 | Gateway | Downstream 서비스 |
|------|---------|------------------|
| 프레임워크 | Spring WebFlux (Reactive) | Spring MVC (Servlet) |
| 스레드 모델 | Netty Event Loop | Tomcat Thread Pool |
| JWT 검증 | O (GlobalFilter) | X (헤더만 읽음) |
| 반환 타입 | `Mono<Void>` | `ResponseEntity<T>` |

---

## 8. K8s 배포

```yaml
# NodePort로 외부 노출
spec:
  type: NodePort
  ports:
    - port: 8000
      targetPort: 8000
      nodePort: 30081
```

**RollingUpdate 전략**:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0  # 무중단 배포
```

---

## 9. 파일 구조

```
service/gateway/
├── build.gradle                          # spring-cloud-gateway + jjwt
├── src/main/resources/application.yml     # 라우팅 + JWT 설정
└── src/main/java/halo/corebridge/gateway/
    ├── GatewayApplication.java
    ├── config/
    │   ├── CorsConfig.java               # CORS 설정
    │   └── JwtProperties.java            # JWT 설정 바인딩
    └── filter/
        └── JwtAuthenticationFilter.java   # 핵심: 3단계 인증 + 헤더 주입
```
