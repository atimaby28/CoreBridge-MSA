// 사용자 역할
export type UserRole = 'ROLE_USER' | 'ROLE_COMPANY' | 'ROLE_ADMIN'

// 사용자 상태
export type UserStatus = 'ACTIVE' | 'BLOCKED' | 'DELETED'

// 사용자 기본 정보
export interface User {
  userId: number
  email: string
  nickname: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

// 관리자용 사용자 정보
export interface AdminUser extends User {
  deletedAt?: string
}

// 로그인 요청
export interface LoginRequest {
  email: string
  password: string
}

// 로그인 응답
export interface LoginResponse {
  userId: number
  email: string
  nickname: string
  role: UserRole
  accessToken: string
  refreshToken: string
}

// 회원가입 요청
export interface SignupRequest {
  email: string
  nickname: string
  password: string
  role?: UserRole
}

// 토큰 갱신 요청
export interface RefreshRequest {
  refreshToken: string
}

// 토큰 응답
export interface TokenResponse {
  accessToken: string
  refreshToken: string
}

// 사용자 정보 수정 요청
export interface UserUpdateRequest {
  nickname?: string
  password?: string
}

// 역할 변경 요청 (Admin)
export interface RoleUpdateRequest {
  role: UserRole
}

// 상태 변경 요청 (Admin)
export interface StatusUpdateRequest {
  status: UserStatus
}

// 사용자 목록 응답 (Admin)
export interface UserPageResponse {
  users: AdminUser[]
  totalCount: number
  page: number
  size: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

// 사용자 통계 응답 (Admin)
export interface UserStatsResponse {
  totalUsers: number
  activeUsers: number
  blockedUsers: number
  adminCount: number
  companyCount: number
  userCount: number
}
