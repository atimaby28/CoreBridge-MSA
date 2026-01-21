import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // ============================================
  // 인증
  // ============================================
  {
    path: '/auth/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/auth/signup',
    name: 'Signup',
    component: () => import('@/views/auth/SignupView.vue'),
    meta: { requiresAuth: false },
  },

  // ============================================
  // 메인
  // ============================================
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true },
  },

  // ============================================
  // 관리자 (ROLE_ADMIN)
  // ============================================
  {
    path: '/admin/users',
    name: 'AdminUserList',
    component: () => import('@/views/admin/UserManageView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/stats',
    name: 'AdminStats',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/audits',
    name: 'AdminAudits',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },

  // ============================================
  // 기업 (ROLE_COMPANY)
  // ============================================
  {
    path: '/company/jobpostings',
    name: 'CompanyJobpostings',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresCompany: true },
  },
  {
    path: '/company/jobpostings/new',
    name: 'CompanyJobpostingNew',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresCompany: true },
  },
  {
    path: '/company/applications',
    name: 'CompanyApplications',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresCompany: true },
  },
  {
    path: '/company/schedules',
    name: 'CompanySchedules',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresCompany: true },
  },
  {
    path: '/company/profile',
    name: 'CompanyProfile',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true, requiresCompany: true },
  },

  // ============================================
  // 일반 사용자 (ROLE_USER)
  // ============================================
  {
    path: '/jobpostings',
    name: 'JobpostingList',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/my/applications',
    name: 'MyApplications',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/my/schedules',
    name: 'MySchedules',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/my/resume',
    name: 'MyResume',
    component: () => import('@/views/PlaceholderView.vue'),
    meta: { requiresAuth: true },
  },

  // 404
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 인증 가드 (Pinia store 사용)
router.beforeEach(async (to, _from, next) => {
  // Pinia store 동적 import (순환 참조 방지)
  const { useAuthStore } = await import('@/stores/auth')
  const authStore = useAuthStore()
  
  // 초기화 대기
  if (!authStore.initialized) {
    await authStore.initialize()
  }
  
  const isAuthenticated = authStore.isAuthenticated
  const requiresAuth = to.meta.requiresAuth !== false

  // 비로그인 상태로 루트(/) 접근 시 /home으로
  if (!isAuthenticated && to.path === '/') {
    next('/home')
  }
  // 인증 필요한 페이지에 비로그인 접근 시
  else if (requiresAuth && !isAuthenticated) {
    next(`/auth/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
  // 로그인 상태로 auth 페이지 접근 시
  else if (!requiresAuth && isAuthenticated && to.path.startsWith('/auth')) {
    next('/')
  }
  // 로그인 상태로 /home 접근 시 대시보드로
  else if (isAuthenticated && to.path === '/home') {
    next('/')
  }
  // Admin 페이지 권한 체크
  else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/')
  }
  // Company 페이지 권한 체크
  else if (to.meta.requiresCompany && !authStore.isCompany) {
    next('/')
  }
  else {
    next()
  }
})

export default router
