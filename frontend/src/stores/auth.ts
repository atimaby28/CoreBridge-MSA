import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService, userService } from '@/api/user'
import type { User, LoginRequest, SignupRequest, UserRole } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // ============================================
  // State
  // ============================================
  const user = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const initialized = ref(false)

  // ============================================
  // Getters
  // ============================================
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ROLE_ADMIN')
  const isCompany = computed(() => user.value?.role === 'ROLE_COMPANY')
  const isUser = computed(() => user.value?.role === 'ROLE_USER')
  const isVisitor = computed(() => !user.value)
  const userId = computed(() => user.value?.userId ?? null)
  const userRole = computed<UserRole | null>(() => user.value?.role ?? null)

  // ============================================
  // Actions
  // ============================================

  // 로그인
  async function login(credentials: LoginRequest): Promise<User> {
    loading.value = true
    error.value = null
    
    try {
      const response = await authService.login(credentials)
      
      // 서버가 Cookie로 토큰 설정, 응답에서 사용자 정보만 받음
      user.value = {
        userId: response.userId,
        email: response.email,
        nickname: response.nickname,
        role: response.role,
        status: 'ACTIVE',
        createdAt: '',
        updatedAt: '',
      }
      
      return user.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '로그인에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 회원가입
  async function signup(data: SignupRequest): Promise<User> {
    loading.value = true
    error.value = null
    
    try {
      const response = await authService.signup(data)
      return response
    } catch (e) {
      error.value = e instanceof Error ? e.message : '회원가입에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 로그아웃
  async function logout(): Promise<void> {
    try {
      if (user.value) {
        await authService.logout()
      }
    } catch {
      // 로그아웃 API 실패해도 로컬 정리
    } finally {
      clearAuth()
    }
  }

  // 인증 정보 초기화
  function clearAuth(): void {
    user.value = null
  }

  // 사용자 정보 조회 (Cookie 기반 인증 확인)
  async function fetchUser(): Promise<void> {
    loading.value = true
    
    try {
      user.value = await userService.getMe()
    } catch {
      // 토큰 만료 또는 미인증 상태
      clearAuth()
    } finally {
      loading.value = false
    }
  }

  // 사용자 정보 수정
  async function updateUser(data: { nickname?: string; password?: string }): Promise<User> {
    if (!user.value) {
      throw new Error('로그인이 필요합니다.')
    }
    
    loading.value = true
    error.value = null
    
    try {
      const updated = await userService.updateMe(data)
      user.value = updated
      return updated
    } catch (e) {
      error.value = e instanceof Error ? e.message : '수정에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 회원 탈퇴
  async function deleteAccount(): Promise<void> {
    if (!user.value) {
      throw new Error('로그인이 필요합니다.')
    }
    
    loading.value = true
    
    try {
      await userService.deleteMe()
      clearAuth()
    } catch (e) {
      error.value = e instanceof Error ? e.message : '탈퇴에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 에러 초기화
  function clearError(): void {
    error.value = null
  }

  // 앱 초기화 (Cookie가 있으면 사용자 정보 조회)
  async function initialize(): Promise<void> {
    if (initialized.value) return
    
    try {
      await fetchUser()
    } catch {
      // 미인증 상태 - 정상
    } finally {
      initialized.value = true
    }
  }

  return {
    // State
    user,
    loading,
    error,
    initialized,
    
    // Getters
    isAuthenticated,
    isAdmin,
    isCompany,
    isUser,
    isVisitor,
    userId,
    userRole,
    
    // Actions
    login,
    signup,
    logout,
    clearAuth,
    fetchUser,
    updateUser,
    deleteAccount,
    clearError,
    initialize,
  }
})
