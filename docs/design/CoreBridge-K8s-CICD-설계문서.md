# CoreBridge K8s + CI/CD 설계 문서

## 1. 개요

### 목적
K3s(경량 Kubernetes) 기반 컨테이너 오케스트레이션 + Jenkins CI/CD 파이프라인으로
MSA 13개 서비스의 자동 빌드, 배포, 모니터링을 구현한다.

### 구현 상태: ✅ 완료
### 인프라 환경: 로컬 WSL2 + K3s (삼성550XDA, 16GB RAM, GPU 없음)

---

## 2. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    K3s Cluster (WSL2)                        │
│                                                             │
│  ┌─── namespace: corebridge ──────────────────────────────┐ │
│  │                                                         │ │
│  │  [gateway]  [user]  [jobposting]  [comment]            │ │
│  │  [view]  [like]  [hot]  [read]                         │ │
│  │  [apply]  [resume]  [schedule]  [notification]         │ │
│  │  [admin-audit]  [frontend]                             │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─── namespace: jenkins ─────────┐                         │
│  │  [Jenkins + Kaniko]            │                         │
│  │  → Git Pull → Build → Push    │                         │
│  │  → K8s Rolling Update         │                         │
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

## 3. K3s 클러스터 구성

### 3-1. Namespace 설계

| Namespace | 용도 | Pod 수 |
|-----------|------|--------|
| `corebridge` | 백엔드 13개 + 프론트엔드 1개 | 14 |
| `jenkins` | CI/CD 파이프라인 | 1 |
| `monitoring` | Prometheus + Grafana | 2 |

### 3-2. 서비스별 K8s 리소스

각 서비스는 동일한 구조의 yaml로 배포:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: corebridge-{service}
  namespace: corebridge
spec:
  replicas: 1
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0      # 무중단 배포
  template:
    spec:
      containers:
        - name: {service}
          image: atimaby12/corebridge-{service}:latest
          ports:
            - containerPort: {port}
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:       # 트래픽 수신 준비 확인
            httpGet:
              path: /actuator/health
              port: {port}
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:        # 컨테이너 생존 확인
            httpGet:
              path: /actuator/health
              port: {port}
            initialDelaySeconds: 60
            periodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: corebridge-{service}
  namespace: corebridge
spec:
  type: ClusterIP             # 내부 통신
  selector:
    app: corebridge-{service}
  ports:
    - port: {port}
      targetPort: {port}
```

### 3-3. 서비스 포트 매핑

| 서비스 | 컨테이너 포트 | K8s Service 타입 | NodePort |
|--------|-------------|-----------------|----------|
| gateway | 8000 | **NodePort** | 30081 |
| user | 8001 | ClusterIP | - |
| jobposting | 8002 | ClusterIP | - |
| comment | 8003 | ClusterIP | - |
| view | 8004 | ClusterIP | - |
| like | 8005 | ClusterIP | - |
| hot | 8006 | ClusterIP | - |
| read | 8007 | ClusterIP | - |
| resume | 8008 | ClusterIP | - |
| apply | 8009 | ClusterIP | - |
| notification | 8011 | ClusterIP | - |
| schedule | 8012 | ClusterIP | - |
| admin-audit | 8012 | ClusterIP | - |
| frontend | 80 | **NodePort** | 30080 |

**외부 접근**: Gateway(30081)와 Frontend(30080)만 NodePort로 노출

---

## 4. Jenkins CI/CD 파이프라인

### 4-1. Jenkins 배포

```yaml
# deploy/k3s/jenkins/
├── deployment.yaml    # Jenkins 컨테이너 + 볼륨
├── service.yaml       # NodePort 노출
└── rbac.yaml          # K8s API 접근 권한
```

### 4-2. 파이프라인 흐름

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

### 4-3. Kaniko (클러스터 내 Docker Build)

K3s 내에서 Docker 데몬 없이 컨테이너 이미지를 빌드할 수 있는 도구.

```
Jenkins Pod → Kaniko Container → DockerHub Push
```

**Docker-in-Docker 대신 Kaniko를 사용하는 이유:**
- DinD는 privileged 모드 필요 → 보안 취약
- Kaniko는 일반 사용자 권한으로 실행 가능

### 4-4. RollingUpdate 전략

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1         # 새 Pod 1개 먼저 생성
    maxUnavailable: 0   # 기존 Pod는 새 Pod Ready 전까지 유지
```

**무중단 배포 흐름:**
1. 새 Pod 생성 (v2)
2. readinessProbe 통과 대기
3. 통과 → Service 엔드포인트에 v2 추가
4. 기존 Pod (v1) 제거

---

## 5. 모니터링 (Prometheus + Grafana)

### 5-1. Prometheus

```yaml
# deploy/k3s/monitoring/
├── prometheus-config.yaml      # scrape 설정
├── prometheus-deployment.yaml
├── prometheus-service.yaml
└── prometheus-rbac.yaml        # K8s 메트릭 수집 권한
```

**Scrape 대상:**
- 각 Spring Boot 서비스의 `/actuator/prometheus`
- FastAPI의 `/metrics`
- K8s 노드/Pod 메트릭 (kube-state-metrics)

### 5-2. Grafana

```yaml
├── grafana-deployment.yaml
├── grafana-service.yaml
└── grafana-dashboard.json      # 사전 설정 대시보드
```

**대시보드 패널:**
- 서비스별 응답시간 (p50, p95, p99)
- 서비스별 요청 수 / 에러율
- Pod CPU/Memory 사용량
- AI Pipeline 처리 시간
- CircuitBreaker 상태 (추후 추가)

---

## 6. 인프라 분리 전략

### 로컬 PC 제약사항
- 삼성550XDA, Intel 11세대, RAM 16GB, GPU 없음
- K3s + 14개 Pod + Jenkins + Prometheus + Grafana 동시 실행 시 리소스 한계

### 분리 방안
```
K3s (WSL2):
  - Spring Boot 서비스 13개 + Frontend
  - Jenkins + Prometheus + Grafana

Docker Compose (WSL2, K3s 외부):
  - FastAPI + Redis Stack + Ollama + n8n
  - AI Pipeline 전용 (CPU 집약적)
```

**이유**: Ollama LLM 추론이 CPU를 많이 소모하여 K3s 클러스터와 리소스 경합 발생.
Docker Compose로 분리하면 독립적으로 리소스 관리 가능.

---

## 7. 부하 테스트

### test_before_load.sh / test_after_load.sh
```bash
# AI Pipeline 엔드포인트별 반복 호출
# Part A: Ollama 미사용 (빠른 엔드포인트)
#   /save_resume, /save_jobposting, /match_*
# Part B: Ollama 사용 (느린 엔드포인트)
#   /skills, /score, /skill_gap, /summary

# 결과를 Prometheus 메트릭으로 수집 → Grafana에서 Before/After 비교
```

---

## 8. 포트폴리오 캡처 항목

| 항목 | 캡처 내용 |
|------|----------|
| K8s Pod 상태 | `kubectl get pods -n corebridge` — 14개 Running |
| RollingUpdate | 새 버전 배포 시 무중단 전환 과정 |
| Jenkins 파이프라인 | Build → Push → Deploy 성공 화면 |
| Grafana 대시보드 | 서비스별 응답시간, CPU/Memory, AI 메트릭 |
| Prometheus Targets | 모든 서비스 scrape 상태 UP |

---

## 9. 파일 구조

```
deploy/
├── k3s/
│   ├── README.md
│   ├── dashboard/
│   │   ├── dashboard-admin.yaml
│   │   └── dashboard-service.yaml
│   ├── jenkins/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── rbac.yaml
│   ├── monitoring/
│   │   ├── prometheus-config.yaml
│   │   ├── prometheus-deployment.yaml
│   │   ├── prometheus-service.yaml
│   │   ├── prometheus-rbac.yaml
│   │   ├── grafana-deployment.yaml
│   │   ├── grafana-service.yaml
│   │   ├── grafana-dashboard.json
│   │   └── test_load.sh
│   └── services/
│       ├── backend/
│       │   ├── gateway.yaml
│       │   ├── user.yaml
│       │   ├── jobposting.yaml
│       │   ├── jobposting-comment.yaml
│       │   ├── jobposting-view.yaml
│       │   ├── jobposting-like.yaml
│       │   ├── jobposting-hot.yaml
│       │   ├── jobposting-read.yaml
│       │   ├── apply.yaml
│       │   ├── resume.yaml
│       │   ├── schedule.yaml
│       │   ├── notification.yaml
│       │   └── admin-audit.yaml
│       └── frontend/
│           └── frontend.yaml
└── load-test/
    ├── test_before_load.sh
    └── test_after_load.sh
```
