# Frontend — CoreBridge

Vue 3 + TypeScript + Pinia + Tailwind CSS 기반 SPA

## 기술 스택

- **Vue 3** (Composition API)
- **TypeScript 5.x**
- **Pinia** (상태 관리)
- **Vue Router** (라우팅)
- **Axios** (HTTP 클라이언트 — Gateway 단일 진입점)
- **Tailwind CSS** (스타일링)
- **Vite** (빌드 도구)

## 프로젝트 구조

```
src/
├── api/                        # API 서비스 (Gateway 경유)
│   ├── index.ts               # Axios 인스턴스 + 인터셉터 + Refresh Token 자동 갱신
│   ├── user.ts                # 사용자/인증 API
│   ├── jobposting.ts          # 채용공고 API
│   ├── apply.ts               # 지원서 API
│   ├── resume.ts              # 이력서 API
│   ├── schedule.ts            # 면접 일정 API
│   ├── notification.ts        # 알림 API
│   ├── audit.ts               # 감사 로그 API
│   └── aiMatching.ts          # AI 매칭 API
│
├── components/
│   ├── common/
│   │   ├── AppHeader.vue      # 헤더 (알림 드롭다운 포함)
│   │   ├── AppSidebar.vue     # 사이드바 (역할별 메뉴)
│   │   └── SkillTagInput.vue  # 스킬 태그 입력
│   ├── ai/
│   │   ├── AiMatchingResult.vue
│   │   ├── AiScoreDetail.vue
│   │   └── AiSkillGap.vue
│   ├── notification/
│   │   └── NotificationDropdown.vue
│   └── schedule/
│       ├── ScheduleCreateModal.vue
│       └── ScheduleModal.vue
│
├── stores/                     # Pinia 상태 관리
│   ├── auth.ts                # 인증 (JWT, 역할)
│   ├── jobposting.ts          # 채용공고
│   ├── apply.ts               # 지원서
│   ├── resume.ts              # 이력서
│   ├── schedule.ts            # 면접 일정
│   └── notification.ts        # 알림
│
├── types/                      # TypeScript 타입 정의
│   ├── user.ts / jobposting.ts / apply.ts / resume.ts
│   ├── schedule.ts / notification.ts / aiMatching.ts
│   └── common.ts / index.ts
│
├── views/
│   ├── HomeView.vue           # 랜딩 페이지
│   ├── DashboardView.vue      # 대시보드
│   ├── ProfileView.vue        # 프로필
│   ├── auth/                  # 로그인, 회원가입
│   ├── jobposting/            # 채용공고 목록, 상세, 작성, 내 공고
│   ├── apply/                 # 지원자 관리(칸반보드), 내 지원 목록
│   ├── resume/                # 이력서 관리
│   ├── schedule/              # 면접 일정
│   ├── notification/          # 알림 목록
│   ├── ai/                    # AI 매칭 결과, AI 추천
│   └── admin/                 # 사용자 관리, 감사 로그
│
├── composables/
│   └── useDate.ts             # 날짜 포맷 유틸리티
│
├── router/
│   └── index.ts               # 라우터 설정 (역할별 가드)
│
├── App.vue
└── main.ts
```

## 개발 환경 실행

```bash
npm install
npm run dev
# → http://localhost:5173
```

## 환경 변수

```
VITE_GATEWAY_URL=http://localhost:8000
```

> 모든 API 요청은 Gateway(:8000)를 단일 진입점으로 사용. Vite dev server에서는 proxy 설정으로 CORS 해결.

## 주요 기능

- **JWT 인증**: Cookie 기반 Access/Refresh Token, 401 시 자동 갱신, 갱신 실패 시 로그아웃
- **역할 기반 라우팅**: USER(구직자), COMPANY(기업), ADMIN(관리자) 별 메뉴/페이지 분기
- **채용공고**: 목록 조회, 상세(조회수/좋아요/댓글 포함), 작성/수정
- **지원자 관리**: 칸반보드 드래그앤드롭 상태 변경, 일괄 처리
- **이력서 관리**: 이력서 CRUD
- **AI 매칭**: 매칭 결과 조회, 스킬갭 분석, AI 추천
- **면접 일정**: 일정 생성/조회
- **알림**: 알림 목록, 읽음 처리
- **감사 로그**: 관리자 전용 API 요청 이력 조회

## 빌드 & 배포

```bash
# 프로덕션 빌드
npm run build

# Docker 빌드 (Nginx 기반)
docker build -t corebridge-frontend .
```

Nginx 설정(`nginx.conf`)으로 SPA 라우팅 + API 프록시 처리.
