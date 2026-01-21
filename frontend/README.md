# CoreBridge Frontend

Vue 3 + TypeScript + Pinia + Tailwind CSS 기반 프론트엔드

## 기술 스택

- **Vue 3** (Composition API)
- **TypeScript**
- **Pinia** (상태 관리)
- **Vue Router** (라우팅)
- **Axios** (HTTP 클라이언트)
- **Tailwind CSS** (스타일링)
- **Vite** (빌드 도구)

## 프로젝트 구조

```
src/
├── api/                    # API 서비스
│   ├── index.ts           # Axios 인스턴스 + 인터셉터
│   └── user.ts            # User API
├── assets/
│   └── main.css           # Tailwind + 전역 스타일
├── components/
│   └── common/
│       ├── AppHeader.vue  # 헤더 컴포넌트
│       └── AppSidebar.vue # 사이드바 컴포넌트
├── composables/
│   └── useDate.ts         # 날짜 유틸리티
├── router/
│   └── index.ts           # 라우터 설정
├── stores/
│   └── auth.ts            # 인증 스토어
├── types/
│   ├── common.ts          # 공통 타입
│   ├── user.ts            # User 타입
│   └── index.ts           # Export
├── views/
│   ├── auth/
│   │   ├── LoginView.vue
│   │   └── SignupView.vue
│   ├── admin/
│   │   └── UserManageView.vue
│   ├── DashboardView.vue
│   └── HomeView.vue
├── App.vue
└── main.ts
```

## 개발 환경 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build
```

## 환경 변수

`.env` 파일 설정:

```
VITE_USER_API_URL=http://localhost:8081
```

## 주요 기능

### 구현 완료
- ✅ JWT 인증 (로그인, 회원가입, 토큰 갱신)
- ✅ 역할 기반 라우팅 (USER, COMPANY, ADMIN)
- ✅ 관리자 사용자 관리
- ✅ 반응형 레이아웃

### TODO (백엔드 구현 후)
- ⏳ 채용공고 관리
- ⏳ 지원 관리
- ⏳ 이력서 관리
- ⏳ 면접 일정 관리
- ⏳ 알림

## JWT 인증 흐름

```
1. 로그인 → Access Token + Refresh Token 발급
2. API 요청 → Authorization 헤더에 Access Token
3. 401 응답 → Refresh Token으로 자동 갱신
4. 갱신 실패 → 로그아웃 + 로그인 페이지
```

## 역할별 접근 권한

| 페이지 | VISITOR | USER | COMPANY | ADMIN |
|-------|---------|------|---------|-------|
| /home | ✅ | - | - | - |
| / (대시보드) | - | ✅ | ✅ | ✅ |
| /auth/* | ✅ | - | - | - |
| /admin/* | - | - | - | ✅ |
