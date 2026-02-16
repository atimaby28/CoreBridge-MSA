# CoreBridge K8s + CI/CD 설계 문서

## 1. 개요

### 목적
K3s(경량 Kubernetes) 기반 컨테이너 오케스트레이션 + Jenkins CI/CD 파이프라인으로
MSA 13개 서비스의 자동 빌드, 배포, 모니터링을 구현한다.

### 구현 출처
kuke-board에 없는 패턴으로, **완전 자체 설계 및 구현**.
K3s 클러스터 구성, Jenkins + Kaniko 파이프라인, Prometheus + Grafana 모니터링 모두 신규.

### 구현 상태: ✅ 완료
### 인프라 환경: 로컬 WSL2 + K3s

---

## 2. 왜 K3s인가?

### Kubernetes 환경 비교

| 항목 | minikube | kind | **K3s** | EKS |
|------|----------|------|---------|-----|
| 메모리 오버헤드 | 2GB+ (VM 기반) | 1.5GB+ | **512MB** | 관리형 (EC2) |
| K8s API 호환 | ✅ | ✅ | ✅ | ✅ |
| 프로덕션 전환 | ❌ (개발용) | ❌ (테스트용) | **✅ (Rancher/RKE2)** | ✅ |
| 16GB RAM에서 14Pod+Jenkins+모니터링 | ❌ 부족 | △ 빡빡 | **✅ 가능** | N/A (클라우드) |
| YAML 호환 | ✅ | ✅ | ✅ | ✅ |

**선택 근거:**
- 16GB RAM에서 14개 Pod + Jenkins + Prometheus + Grafana를 동시 운영해야 하는 제약
- minikube는 VM 기반이라 메모리 오버헤드가 커서 감당 불가
- kind는 CI 테스트용으로 설계되어 프로덕션 패턴 학습에 부적합
- **K3s는 풀 K8s API를 지원하면서 메모리 풋프린트가 가장 작음**
- EKS 전환 시에도 Deployment/Service YAML을 그대로 사용 가능 (K8s API 호환)

---

## 3. 왜 Rolling Update인가? (배포 전략 선택)

| 전략 | 장점 | 단점 | 리소스 | 선택 |
|------|------|------|--------|------|
| **Rolling Update** | 점진적 교체, 리소스 효율적 | 버전 혼재 구간 존재 | **1x + maxSurge(1)** | ✅ |
| Blue-Green | 즉시 전환/롤백 | **리소스 2배** 필요 | 2x | ❌ |
| Canary | 위험 최소화, 점진적 검증 | 트래픽 분할 설정 복잡 | 1x + α | ❌ |

**Rolling Update 선택 근거:**
- 16GB RAM 제약으로 Blue-Green의 리소스 2배는 감당 불가 (14개 서비스 × 2 = 28 Pod)
- readinessProbe로 새 Pod이 준비되기 전까지 트래픽을 보내지 않으므로 **실질적 무중단**
- `maxSurge=1, maxUnavailable=0` 설정으로 기존 Pod는 새 Pod Ready 이후에만 제거

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1         # 새 Pod 1개 먼저 생성
    maxUnavailable: 0   # 기존 Pod는 새 Pod Ready 전까지 유지
```

**무중단 배포 흐름:**
1. 새 Pod 생성 (v2)
2. readinessProbe 통과 대기 (`/actuator/health`, initialDelay=30s)
3. 통과 → Service 엔드포인트에 v2 추가
4. 기존 Pod (v1) Graceful Shutdown (terminationGracePeriodSeconds=60)

> **참고**: 이전 Hanwha BEYOND SW Camp 프로젝트에서 Blue-Green/Canary 배포를 경험. CoreBridge에서는 리소스 제약을 고려하여 Rolling Update를 선택했으며, EKS 전환 시 Helm + ArgoCD(GitOps) 기반 Blue-Green 도입 계획.

---

## 4. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    K3s Cluster (WSL2)                        │
│                                                             │
│  ┌─── namespace: corebridge ──────────────────────────────┐ │
│  │  [gateway]  [user]  [jobposting]  [comment]            │ │
│  │  [view]  [like]  [hot]  [read]                         │ │
│  │  [apply]  [resume]  [schedule]  [notification]         │ │
│  │  [admin-audit]  [frontend]                             │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─── namespace: jenkins ─────────┐                         │
│  │  [Jenkins + Kaniko]            │                         │
│  └────────────────────────────────┘                         │
│                                                             │
│  ┌─── namespace: monitoring ──────┐                         │
│  │  [Prometheus]  [Grafana]       │                         │
│  └────────────────────────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─── K3s 외부 (Docker Compose) ────┐
│  [FastAPI]  [Ollama]  [Redis]    │  ← AI Pipeline (리소스 분리)
│  [n8n]                            │
└───────────────────────────────────┘
```

---

## 5. Jenkins CI/CD 파이프라인

### 왜 Kaniko인가? (Docker-in-Docker 대비)

| 방식 | 보안 | 성능 | K3s 호환 |
|------|------|------|---------|
| Docker-in-Docker (DinD) | ❌ privileged 모드 필요 | 중간 | △ |
| **Kaniko** | ✅ 일반 사용자 권한 | 빠름 (레이어 캐시) | ✅ |

DinD는 privileged 모드가 필요하여 보안 취약. Kaniko는 일반 Pod에서 Docker 데몬 없이 이미지 빌드 가능.

### 파이프라인 흐름

```
[Git Push] → [Jenkins Webhook]
                ↓
         [1. Git Checkout]
                ↓
         [2. Gradle Build] → bootJar
                ↓
         [3. Kaniko Build] → Docker Image
                ↓
         [4. Push to DockerHub] → atimaby12/corebridge-{service}:latest
                ↓
         [5. kubectl rollout restart] → K8s RollingUpdate
```

---

## 6. 서비스 포트 매핑

| 서비스 | 컨테이너 포트 | K8s Service 타입 | NodePort |
|--------|-------------|-----------------|----------|
| gateway | 8000 | **NodePort** | 30081 |
| frontend | 80 | **NodePort** | 30080 |
| 나머지 12개 | 각 서비스 포트 | ClusterIP | - |

**외부 접근**: Gateway(30081)와 Frontend(30080)만 NodePort로 노출.
나머지 서비스는 ClusterIP로 클러스터 내부에서만 접근 가능.

---

## 7. 리소스 설정

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**requests/limits 설정 근거:**
- 16GB RAM에서 14개 Pod 운영 → Pod당 최대 512Mi로 제한
- Spring Boot 서비스의 평균 힙 사용량 ~200~300MB를 고려
- `JAVA_OPTS: -Xms128m -Xmx384m`으로 JVM 힙 크기 명시

---

## 8. 모니터링 (Prometheus + Grafana)

### Scrape 대상
- 각 Spring Boot 서비스: `/actuator/prometheus`
- FastAPI: `/metrics`
- Resilience4j CircuitBreaker 메트릭 자동 수집

### Grafana 대시보드 패널
- 서비스별 응답시간 (p50, p95, p99)
- 서비스별 요청 수 / 에러율
- Pod CPU/Memory 사용량
- AI Pipeline 처리 시간
- CircuitBreaker 상태 변화 타임라인

---

## 9. 포트폴리오 캡처 항목

| 항목 | 캡처 내용 |
|------|----------|
| K8s Pod 상태 | `kubectl get pods -n corebridge` — 14개 Running |
| RollingUpdate | 새 버전 배포 시 무중단 전환 과정 |
| Jenkins 파이프라인 | Build → Push → Deploy 성공 화면 |
| Grafana 대시보드 | 서비스별 응답시간, CPU/Memory, AI 메트릭 |
| Prometheus Targets | 모든 서비스 scrape 상태 UP |

---

## 10. 파일 구조

```
deploy/
├── k3s/
│   ├── services/backend/        # 13개 서비스 Deployment + Service YAML
│   ├── services/frontend/       # 프론트엔드 K8s 배포
│   ├── jenkins/                 # Jenkins Deployment + RBAC
│   ├── monitoring/              # Prometheus + Grafana + 대시보드
│   └── dashboard/               # K8s Dashboard
└── load-test/
    ├── test_before_load.sh      # Before: 동기 호출 부하 테스트
    └── test_after_load.sh       # After: 비동기 파이프라인 부하 테스트
```
