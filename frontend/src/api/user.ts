import { userApi } from './index'
import type { 
  User, 
  AdminUser,
  LoginRequest, 
  LoginResponse, 
  SignupRequest, 
  UserUpdateRequest,
  RoleUpdateRequest,
  StatusUpdateRequest,
  UserPageResponse,
  UserStatsResponse,
  UserRole,
  RefreshRequest,
  TokenResponse,
} from '@/types'

// ============================================
// 인증 API
// ============================================
export const authService = {
  // 회원가입
  async signup(data: SignupRequest): Promise<User> {
    return userApi.post('/api/v1/users/signup', data)
  },

  // 로그인
  async login(data: LoginRequest): Promise<LoginResponse> {
    return userApi.post('/api/v1/users/login', data)
  },

  // 토큰 갱신
  async refresh(data: RefreshRequest): Promise<TokenResponse> {
    return userApi.post('/api/v1/users/refresh', data)
  },

  // 로그아웃
  async logout(): Promise<void> {
    return userApi.post('/api/v1/users/logout')
  },
}

// ============================================
// 사용자 API
// ============================================
export const userService = {
  // 내 정보 조회
  async getMe(): Promise<User> {
    return userApi.get('/api/v1/users/me')
  },

  // 내 정보 수정
  async updateMe(data: UserUpdateRequest): Promise<User> {
    return userApi.put('/api/v1/users/me', data)
  },

  // 회원 탈퇴
  async deleteMe(): Promise<void> {
    return userApi.delete('/api/v1/users/me')
  },

  // 특정 사용자 조회 (공개 정보)
  async getUser(userId: number): Promise<User> {
    return userApi.get(`/api/v1/users/${userId}`)
  },
}

// ============================================
// 관리자 API
// ============================================
export const adminUserService = {
  // 전체 사용자 목록 조회
  async getAllUsers(page: number = 0, size: number = 20): Promise<UserPageResponse> {
    return userApi.get('/api/v1/users/admin/list', {
      params: { page, size }
    })
  },

  // 역할별 사용자 목록 조회
  async getUsersByRole(role: UserRole, page: number = 0, size: number = 20): Promise<UserPageResponse> {
    return userApi.get(`/api/v1/users/admin/list/role/${role}`, {
      params: { page, size }
    })
  },

  // 사용자 검색
  async searchUsers(keyword: string, page: number = 0, size: number = 20): Promise<UserPageResponse> {
    return userApi.get('/api/v1/users/admin/search', {
      params: { keyword, page, size }
    })
  },

  // 특정 사용자 상세 조회 (관리자)
  async getUserDetail(userId: number): Promise<AdminUser> {
    return userApi.get(`/api/v1/users/admin/${userId}`)
  },

  // 역할 변경
  async updateRole(userId: number, data: RoleUpdateRequest): Promise<AdminUser> {
    return userApi.patch(`/api/v1/users/admin/${userId}/role`, data)
  },

  // 상태 변경
  async updateStatus(userId: number, data: StatusUpdateRequest): Promise<AdminUser> {
    return userApi.patch(`/api/v1/users/admin/${userId}/status`, data)
  },

  // 사용자 통계 조회
  async getStats(): Promise<UserStatsResponse> {
    return userApi.get('/api/v1/users/admin/stats')
  },
}
