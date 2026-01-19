# CI/CD & Kubernetes 구축 회고

## 배경

MSA로 13개 서비스를 운영하면서:
- 수동 배포는 **시간 낭비 + 휴먼 에러**
- 배포 중 **서비스 중단** 발생
- 장애 발생 시 **원인 파악 어려움**

**목표**: 자동화된 CI/CD + 무중단 배포 + 실시간 모니터링

## CI/CD 파이프라인 (Jenkins)

### 파이프라인 구성

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  Push   │───▶│  Build  │───▶│  Test   │───▶│  Image  │
│ (Git)   │    │ (Gradle)│    │ (JUnit) │    │ (Kaniko)│
└─────────┘    └─────────┘    └─────────┘    └─────────┘
                                                  │
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌───▼─────┐
│ Cleanup │◀───│ Switch  │◀───│ Health  │◀───│ Deploy  │
│ (Blue)  │    │ Service │    │ Check   │    │ (Green) │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
```

### Jenkinsfile 핵심 부분

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build -x test'
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }
        
        stage('Docker Build') {
            steps {
                // Kaniko로 이미지 빌드 (Docker-in-Docker 대신)
                container('kaniko') {
                    sh '''
                        /kaniko/executor \
                            --dockerfile=Dockerfile \
                            --destination=${REGISTRY}/${IMAGE}:${BUILD_NUMBER}
                    '''
                }
            }
        }
        
        stage('Deploy Green') {
            steps {
                sh '''
                    kubectl set image deployment/${APP}-green \
                        ${APP}=${REGISTRY}/${IMAGE}:${BUILD_NUMBER}
                    kubectl rollout status deployment/${APP}-green
                '''
            }
        }
        
        stage('Switch Traffic') {
            steps {
                sh '''
                    kubectl patch service ${APP} \
                        -p '{"spec":{"selector":{"version":"green"}}}'
                '''
            }
        }
    }
}
```

## Blue-Green 배포

### 왜 Blue-Green인가?

| 전략 | 장점 | 단점 | 선택 |
|------|------|------|------|
| Rolling | 점진적 | 버전 혼재 | ❌ |
| Canary | 위험 최소화 | 복잡 | ❌ |
| **Blue-Green** | 즉시 전환/롤백 | 리소스 2배 | ✅ |

**선택 이유:**
- 채용 시스템은 **데이터 정합성**이 중요
- 버전 혼재 시 **상태 전이 문제** 발생 가능
- 문제 시 **즉시 롤백** 가능

### 배포 흐름

```
1. 현재 상태: Blue (v1) 운영 중
   
   Service ──▶ Blue (v1) ✅
              Green (v1) 대기

2. Green에 새 버전 배포
   
   Service ──▶ Blue (v1) ✅
              Green (v2) 배포 중...

3. Health Check 통과 후 전환
   
   Service ──▶ Blue (v1)
              Green (v2) ✅  ← 트래픽 전환

4. Blue 정리 (또는 롤백 대기)
   
   Service ──▶ Green (v2) ✅
              Blue (v1) 스케일 다운
```

## Kubernetes 구성

### 주요 리소스

```yaml
# Deployment (Blue)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: corebridge-process-blue
spec:
  replicas: 2
  selector:
    matchLabels:
      app: corebridge-process
      version: blue
  template:
    spec:
      containers:
      - name: corebridge-process
        image: registry/corebridge-process:v1
        ports:
        - containerPort: 8004
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8004
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8004
          initialDelaySeconds: 60
          periodSeconds: 30
```

### Probe 설정 중요성

**문제**: 배포 직후 트래픽이 들어와서 에러 발생
**원인**: 앱이 완전히 시작되기 전에 Service가 연결

**해결**: Readiness Probe로 준비 완료 확인
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8004
  initialDelaySeconds: 30  # 앱 시작 대기
  periodSeconds: 10        # 10초마다 확인
```

## 모니터링 (Prometheus + Grafana)

### 수집 메트릭

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'corebridge-process:8004'
        - 'corebridge-notification:8006'
        # ...

  - job_name: 'ai-pipeline'
    static_configs:
      - targets: ['pushgateway:9091']
```

### Grafana 대시보드

| 패널 | 메트릭 | 용도 |
|------|--------|------|
| 요청 수 | `http_server_requests_seconds_count` | 트래픽 모니터링 |
| 응답 시간 | `http_server_requests_seconds_sum` | 지연 감지 |
| 에러율 | `http_server_requests{status=~"5.."}` | 장애 감지 |
| JVM 힙 | `jvm_memory_used_bytes` | 메모리 누수 감지 |
| AI 파이프라인 | `ai_stage_duration_seconds` | 병목 분석 |

## 트러블슈팅

### 문제 1: 배포 후 간헐적 504 에러

**증상**: Blue→Green 전환 직후 일부 요청 타임아웃
**원인**: 기존 Blue로 가던 요청이 완료되기 전에 Blue 종료

**해결**: 
```yaml
# Deployment에 terminationGracePeriodSeconds 추가
spec:
  terminationGracePeriodSeconds: 60  # 60초 대기
```

### 문제 2: CronJob 중복 실행

**증상**: 배치 작업이 동시에 여러 개 실행
**원인**: Pod이 재시작되면서 CronJob 트리거 중복

**해결**:
```yaml
spec:
  concurrencyPolicy: Forbid  # 이전 작업 완료 전까지 새 작업 금지
```

### 문제 3: 메모리 OOM

**증상**: Process 서비스가 갑자기 재시작
**원인**: JVM 힙이 컨테이너 메모리 제한 초과

**해결**:
```yaml
resources:
  limits:
    memory: "1Gi"
  requests:
    memory: "512Mi"

env:
  - name: JAVA_OPTS
    value: "-Xms256m -Xmx512m"  # 힙 크기 명시
```

## 성과

| 지표 | Before | After |
|------|--------|-------|
| 배포 시간 | 30분 (수동) | 5분 (자동) |
| 배포 중 에러 | 발생 | 0% (무중단) |
| 롤백 시간 | 30분 | 1분 (즉시) |
| 장애 감지 | 수동 | 자동 알림 |

## 배운 점

1. **Probe 설정은 필수** → 앱 상태 확인 없이 트래픽 전환은 위험
2. **메트릭 없이 운영 불가** → 문제 원인 파악이 안 됨
3. **Blue-Green은 리소스가 2배** → 비용 vs 안정성 트레이드오프
4. **Graceful Shutdown 중요** → 진행 중인 요청 완료 대기 필요
