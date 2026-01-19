# Frontend - CoreBridge

Vue 3 + TypeScript 기반 SPA

## 기술 스택

- Vue 3 (Composition API)
- TypeScript 5.x
- Pinia (상태 관리)
- Vue Router
- Tailwind CSS
- Vite

## 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build

# 타입 체크
npm run type-check
```

## 주요 화면

| 화면 | 경로 | 설명 |
|------|------|------|
| 대시보드 | / | 역할별 메인 화면 |
| 채용공고 | /jobpostings | 공고 목록/상세 |
| 지원관리 | /applications | 내 지원 현황 |
| 칸반보드 | /company/applicants | 지원자 관리 (기업) |
| 일정 | /schedules | 면접 일정 |
| 알림 | /notifications | 알림 목록 |
| 감사로그 | /admin/audits | 감사 로그 (관리자) |

## 폴더 구조

```
src/
├── api/           # API 서비스
├── components/    # 공통 컴포넌트
├── composables/   # 재사용 로직
├── router/        # 라우팅
├── stores/        # Pinia 스토어
├── types/         # TypeScript 타입
└── views/         # 페이지 컴포넌트
```
