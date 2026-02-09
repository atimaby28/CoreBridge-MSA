# Kubernetes 배포 설정 — CoreBridge

K3s (경량 Kubernetes) + Jenkins + Kaniko 기반 13개 서비스 배포

## 디렉토리 구조

```
k3s/
├── services/
│   ├── backend/             # 13개 백엔드 서비스 Deployment + Service
│   │   ├── gateway.yaml
│   │   ├── user.yaml
│   │   ├── jobposting.yaml
│   │   ├── jobposting-comment.yaml
│   │   ├── jobposting-view.yaml
│   │   ├── jobposting-like.yaml
│   │   ├── jobposting-hot.yaml
│   │   ├── jobposting-read.yaml
│   │   ├── resume.yaml
│   │   ├── apply.yaml
│   │   ├── notification.yaml
│   │   ├── schedule.yaml
│   │   └── admin-audit.yaml
│   └── frontend/            # 프론트엔드 Deployment
│       └── frontend.yaml
│
├── jenkins/                 # Jenkins K8s Pod 배포
│   ├── deployment.yaml
│   ├── service.yaml
│   └── rbac.yaml            # ServiceAccount + ClusterRoleBinding
│
├── monitoring/              # Prometheus + Grafana
│   ├── prometheus-config.yaml
│   ├── prometheus-deployment.yaml
│   ├── prometheus-service.yaml
│   ├── prometheus-rbac.yaml
│   ├── grafana-deployment.yaml
│   ├── grafana-service.yaml
│   └── grafana-dashboard.json
│
└── dashboard/               # K8s Dashboard
    ├── dashboard-admin.yaml
    └── dashboard-service.yaml
```

## 네임스페이스 분리

| Namespace | 용도 |
|-----------|------|
| `corebridge` | 백엔드 13개 서비스 + 프론트엔드 |
| `jenkins` | Jenkins CI/CD |
| `monitoring` | Prometheus + Grafana |

## 배포 방법

```bash
# 네임스페이스 생성
kubectl create namespace corebridge
kubectl create namespace jenkins
kubectl create namespace monitoring

# 백엔드 서비스 배포
kubectl apply -f services/backend/ -n corebridge

# 프론트엔드 배포
kubectl apply -f services/frontend/ -n corebridge

# Jenkins 배포
kubectl apply -f jenkins/ -n jenkins

# 모니터링 배포
kubectl apply -f monitoring/ -n monitoring

# 확인
kubectl get pods -n corebridge
kubectl get services -n corebridge
```

## 배포 전략: Rolling Update

모든 서비스에 **Rolling Update** 전략 적용:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1          # 새 Pod 1개 먼저 생성
    maxUnavailable: 0    # 기존 Pod 유지 → 무중단
```

1. 새 버전 Pod 생성 (maxSurge=1)
2. readinessProbe 통과 확인
3. 기존 Pod 트래픽 차단 후 제거
4. → 서비스 중단 없이 배포 완료

> **참고**: 이전 프로젝트에서 Blue-Green / Canary 배포 경험이 있으며, EKS 전환 시 Helm + ArgoCD(GitOps) 기반 도입 계획.

## 헬스체크 설정

각 서비스에 readinessProbe + livenessProbe 설정:

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: <서비스 포트>
  initialDelaySeconds: 30
  periodSeconds: 10
livenessProbe:
  httpGet:
    path: /actuator/health
    port: <서비스 포트>
  initialDelaySeconds: 60
  periodSeconds: 30
```

## CI/CD 파이프라인 (Jenkins)

```
Git Push → Jenkins(K3s Pod) → Gradle 빌드 → Kaniko 이미지 빌드
  → Docker Hub Push → kubectl set image → Rolling Update
```

- Jenkins가 K3s Pod으로 실행되어 클러스터 내부에서 `kubectl` 직접 실행
- Kaniko로 Docker daemon 없이 이미지 빌드 (보안 + 경량)
- 서비스 선택 배포 (Jenkins parameter: SERVICE_NAME)
