# Kubernetes 배포 설정

## 구조

```
k8s/
├── deployments/      # Deployment 매니페스트
├── services/         # Service 매니페스트
└── configmaps/       # ConfigMap
```

## 배포 방법

```bash
# 네임스페이스 생성
kubectl create namespace corebridge

# ConfigMap 적용
kubectl apply -f configmaps/ -n corebridge

# 배포
kubectl apply -f deployments/ -n corebridge
kubectl apply -f services/ -n corebridge

# 확인
kubectl get pods -n corebridge
kubectl get services -n corebridge
```

## Blue-Green 배포

Jenkins 파이프라인에서 Blue-Green 배포 전략 사용:

1. Green 환경에 새 버전 배포
2. Health Check 통과 확인
3. Service 라우팅 전환 (Blue → Green)
4. Blue 환경 정리
