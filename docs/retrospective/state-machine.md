# State Machine 패턴 도입기

## 배경

채용 프로세스는 복잡한 상태 전이가 필요합니다:
- 지원완료 → 서류검토 → 서류합격/탈락
- 서류합격 → 코딩테스트 or 1차면접
- 각 단계별 합격/탈락 분기

처음에는 if-else로 구현했지만, 상태가 늘어나면서 코드가 복잡해졌습니다.

## 문제점 (Before)

```java
// ❌ 문제가 많은 코드
public void updateStatus(Process process, String newStatus) {
    String current = process.getStatus();
    
    if (current.equals("APPLIED")) {
        if (newStatus.equals("DOCUMENT_REVIEW") || newStatus.equals("DOCUMENT_PASS")) {
            process.setStatus(newStatus);
        } else {
            throw new Exception("Invalid transition");
        }
    } else if (current.equals("DOCUMENT_REVIEW")) {
        if (newStatus.equals("DOCUMENT_PASS") || newStatus.equals("DOCUMENT_FAIL")) {
            process.setStatus(newStatus);
        } else {
            throw new Exception("Invalid transition");
        }
    }
    // ... 계속 늘어남
}
```

**문제:**
1. 상태 추가 시 모든 분기 수정 필요
2. 전이 규칙이 코드에 흩어져 있음
3. 실수로 잘못된 전이 허용 가능
4. 테스트 어려움

## 해결: Enum 기반 State Machine

```java
// ✅ 개선된 코드
public enum ProcessStep {
    APPLIED("지원완료", Set.of("DOCUMENT_REVIEW")),
    DOCUMENT_REVIEW("서류검토중", Set.of("DOCUMENT_PASS", "DOCUMENT_FAIL")),
    DOCUMENT_PASS("서류합격", Set.of("CODING_TEST", "INTERVIEW_1")),
    DOCUMENT_FAIL("서류탈락", Set.of()),  // 종료 상태
    // ...
    
    private final String displayName;
    private final Set<String> allowedNextSteps;
    
    public boolean canTransitionTo(ProcessStep next) {
        return allowedNextSteps.contains(next.name());
    }
    
    public boolean isTerminal() {
        return allowedNextSteps.isEmpty();
    }
}
```

```java
// Entity에서 사용
public class RecruitmentProcess {
    
    public void transition(ProcessStep nextStep) {
        if (!currentStep.canTransitionTo(nextStep)) {
            throw new IllegalStateException(
                "Cannot transition from " + currentStep + " to " + nextStep
            );
        }
        this.previousStep = this.currentStep;
        this.currentStep = nextStep;
        this.stepChangedAt = LocalDateTime.now();
    }
}
```

## 장점

| 항목 | 설명 |
|------|------|
| **타입 안전성** | 컴파일 타임에 유효한 상태만 사용 가능 |
| **명시적 규칙** | 전이 규칙이 Enum에 집중되어 한눈에 파악 |
| **확장성** | 새 상태 추가 시 Enum에만 추가 |
| **테스트 용이** | 규칙이 명확해서 테스트 케이스 작성 쉬움 |

## 대안 검토

| 방식 | 장점 | 단점 | 선택 |
|------|------|------|------|
| if-else | 단순 | 복잡도 증가 | ❌ |
| State 패턴 (GoF) | OCP 준수 | 클래스 폭발 | ❌ |
| Spring Statemachine | 기능 풍부 | 학습곡선, 오버엔지니어링 | ❌ |
| **Enum 기반** | 간결 + 안전 | - | ✅ |

## 배운 점

1. 복잡한 상태 로직은 **명시적인 규칙 정의**가 중요
2. 라이브러리 도입 전에 **직접 구현의 장단점** 고려
3. Enum은 단순한 상수 이상의 역할 가능
