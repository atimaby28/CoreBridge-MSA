# CI/CD & Kubernetes 구축 회고

## 배경

MSA로 13개 서비스를 운영하면서:
- 수동 배포는 **시간 낭비 + 휴먼 에러** (서비스 순서 실수, 설정 누락)
- 배포 중 **서비스 중단** 발생
- 장애 발생 시 **원인 파악 어려움** (13개 서비스 중 어디가 문제인지)

**목표**: 자동화된 CI/CD + 무중단 배포 + 실시간 모니터링

## 배포 전략 선택: Rolling Update

### 왜 Rolling Update인가?

| 전략 | 장점 | 단점 | 리소스 |
|------|------|------|--------|
| **Rolling Update** | 점진적 교체, 리소스 효율적 | 버전 혼재 구간 존재 | 1x + 1 Pod |
| Blue-Green | 즉시 전환/롤백 | 리소스 2배 필요 | 2x |
| Canary | 위험 최소화 | 트래픽 분할 복잡 | 1x + α |

**Rolling Update를 선택한 이유:**
- 16GB RAM에서 14개 서비스 운영 → Blue-Green의 2배 리소스는 물리적으로 불가
- `maxSurge=1, maxUnavailable=0` + `readinessProbe`로 실질적 무중단 배포 달성
- 채용 플랫폼 특성상 초단위 즉시 롤백보다는 안정적인 점진적 교체가 적합

> **참고**: 이전 Hanwha BEYOND SW Camp에서 Blue-Green/Canary 배포를 경험. CoreBridge에서는 로컬 리소스 제약을 고려하여 Rolling Update를 선택했으며, EKS 전환 시 Helm + ArgoCD(GitOps) 기반 Blue-Green 도입 계획.

### Rolling Update 무중단 배포 흐름

```
1. 현재 상태: v1 Pod 운영 중
   Service ──▶ Pod v1 ✅

2. kubectl rollout restart → 새 Pod 생성
   Service ──▶ Pod v1 ✅
               Pod v2 (Starting...)

3. readinessProbe 통과 (30초 대기 + 10초 간격 확인)
   Service ──▶ Pod v1 ✅
               Pod v2 ✅ Ready!

4. v1 Graceful Shutdown (terminationGracePeriodSeconds=60)
   Service ──▶ Pod v2 ✅
               Pod v1 (Terminating... 진행 중인 요청 완료 대기)

5. 완료
   Service ──▶ Pod v2 ✅
```

## CI/CD 파이프라인 (Jenkins + Kaniko)

### 파이프라인 구성

```
[Git Push] → [Jenkins Webhook]
                ↓
         [1. Git Checkout]
                ↓
         [2. Gradle Build] → bootJar (테스트 포함)
                ↓
         [3. Kaniko Build] → Docker Image (멀티스테이지 빌드)
                ↓
         [4. Push to DockerHub] → atimaby12/corebridge-{service}:latest
                ↓
         [5. kubectl rollout restart] → K8s Rolling Update
```

### 왜 Kaniko인가?

Docker-in-Docker(DinD)는 `privileged` 모드가 필요하여 **보안 취약점**.
Kaniko는 일반 사용자 권한으로 컨테이너 이미지를 빌드할 수 있어 K3s 환경에서 안전하게 사용.

## Kubernetes 구성

### Probe 설정의 중요성

**문제**: 배포 직후 트래픽이 들어와서 에러 발생
**원인**: 앱이 완전히 시작되기 전에 Service가 트래픽 전달
**해결**: Readiness Probe로 준비 완료 확인

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8009
  initialDelaySeconds: 30  # Spring Boot 시작 대기
  periodSeconds: 10        # 10초마다 확인

livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8009
  initialDelaySeconds: 60  # 충분한 시작 시간 확보
  periodSeconds: 30
```

### 리소스 설정

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
env:
  - name: JAVA_OPTS
    value: "-Xms128m -Xmx384m"
```

## 모니터링 (Prometheus + Grafana)

### 수집 대상

| 대상 | 경로 | 메트릭 |
|------|------|--------|
| Spring Boot 서비스 | `/actuator/prometheus` | HTTP 요청, JVM, CB 상태 |
| FastAPI | `/metrics` | AI 파이프라인 처리 시간 |
| K8s 노드 | kube-state-metrics | Pod CPU/Memory |

### Grafana 대시보드

| 패널 | 메트릭 | 용도 |
|------|--------|------|
| 요청 수 | `http_server_requests_seconds_count` | 트래픽 모니터링 |
| 응답 시간 | `http_server_requests_seconds_sum` | 지연 감지 |
| 에러율 | `http_server_requests{status=~"5.."}` | 장애 감지 |
| JVM 힙 | `jvm_memory_used_bytes` | 메모리 누수 감지 |
| CircuitBreaker | `resilience4j_circuitbreaker_state` | CB 상태 변화 |
| AI Pipeline | `ai_service_request_latency_seconds` | LLM 병목 분석 |

## 트러블슈팅

### 문제 1: 배포 후 간헐적 504 에러

**증상**: Rolling Update 시 일부 요청 타임아웃
**원인**: 기존 Pod로 가던 진행 중인 요청이 완료되기 전에 Pod 종료
**해결**: `terminationGracePeriodSeconds: 60`으로 Graceful Shutdown 대기

### 문제 2: JVM 메모리 OOM

**증상**: Pod이 갑자기 재시작 (OOMKilled)
**원인**: JVM 힙이 컨테이너 메모리 제한(512Mi) 초과
**해결**: `JAVA_OPTS: -Xms128m -Xmx384m`으로 힙 크기를 limits 이내로 명시

### 문제 3: Spring Boot 시작 지연

**증상**: readinessProbe 실패로 Pod이 재시작 반복
**원인**: Spring Boot + JPA 초기화가 30초 이상 소요
**해결**: `initialDelaySeconds`를 30초(readiness)/60초(liveness)로 조정

## 성과

| 지표 | Before | After |
|------|--------|-------|
| 배포 시간 | 30분 (수동, 13개 서비스 순차) | **5분 (자동)** |
| 배포 중 에러 | 발생 (서비스 중단) | **0% (무중단)** |
| 롤백 | 수동 재배포 30분 | `kubectl rollout undo` **1분** |
| 장애 감지 | 수동 로그 확인 | **Grafana 알림 자동** |

## 배운 점

1. **Probe 설정은 필수** — readinessProbe 없이 Rolling Update는 무중단이 아님
2. **Graceful Shutdown 중요** — terminationGracePeriodSeconds로 진행 중인 요청 완료 대기
3. **JVM 힙 ≤ 컨테이너 limits** — 이 규칙을 어기면 OOMKilled
4. **메트릭 없이 운영 불가** — 13개 서비스의 상태를 사람이 수동으로 파악하는 것은 불가능
