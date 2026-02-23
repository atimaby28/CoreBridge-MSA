import { auditApi } from './index'

// ============================================
// Types
// ============================================

export type AuditEventType =
  | 'LOGIN' | 'LOGOUT' | 'LOGIN_FAILED'
  | 'USER_CREATE' | 'USER_UPDATE' | 'USER_DELETE'
  | 'JOBPOSTING_CREATE' | 'JOBPOSTING_UPDATE' | 'JOBPOSTING_DELETE' | 'JOBPOSTING_READ'
  | 'APPLICATION_CREATE' | 'APPLICATION_CANCEL' | 'APPLICATION_STATUS_CHANGE'
  | 'RESUME_CREATE' | 'RESUME_UPDATE' | 'RESUME_DELETE'
  | 'SCHEDULE_CREATE' | 'SCHEDULE_UPDATE' | 'SCHEDULE_CANCEL'
  | 'NOTIFICATION_READ'
  | 'API_REQUEST'
  | 'SYSTEM_ERROR' | 'UNKNOWN'

export interface AuditResponse {
  auditId: number
  userId: number | null
  userEmail: string | null
  serviceName: string
  eventType: AuditEventType
  eventTypeName: string
  httpMethod: string
  requestUri: string
  clientIp: string
  userAgent: string
  httpStatus: number
  executionTime: number
  requestBody: string | null
  errorMessage: string | null
  createdAt: string
}

export interface AuditPageResponse {
  audits: AuditResponse[]
  totalCount: number
  page: number
  size: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface AuditStatsResponse {
  totalRequests: number
  errorCount: number
  uniqueUsers: number
  avgExecutionTime: number
  mostActiveService: string | null
  mostFrequentEvent: AuditEventType | null
}

// ============================================
// API Functions
// ============================================

/**
 * 페이징된 로그 조회
 */
export async function getAuditsPaged(page = 0, size = 20): Promise<AuditPageResponse> {
  return auditApi.get('/api/v1/admin/audits', { params: { page, size } })
}

/**
 * 최근 로그 조회
 */
export async function getRecentAudits(size = 100): Promise<AuditPageResponse> {
  return auditApi.get('/api/v1/admin/audits/recent', { params: { size } })
}

/**
 * 로그 상세 조회
 */
export async function getAuditById(auditId: number): Promise<AuditResponse> {
  return auditApi.get(`/api/v1/admin/audits/${auditId}`)
}

/**
 * 사용자별 로그 조회
 */
export async function getAuditsByUser(userId: number): Promise<AuditResponse[]> {
  return auditApi.get(`/api/v1/admin/audits/users/${userId}`)
}

/**
 * 서비스별 로그 조회
 */
export async function getAuditsByService(serviceName: string): Promise<AuditResponse[]> {
  return auditApi.get(`/api/v1/admin/audits/services/${serviceName}`)
}

/**
 * 이벤트 타입별 로그 조회
 */
export async function getAuditsByEventType(eventType: AuditEventType): Promise<AuditResponse[]> {
  return auditApi.get(`/api/v1/admin/audits/events/${eventType}`)
}

/**
 * 기간별 로그 조회
 */
export async function getAuditsByDateRange(startDate: string, endDate: string): Promise<AuditResponse[]> {
  return auditApi.get('/api/v1/admin/audits/range', { params: { startDate, endDate } })
}

/**
 * 에러 로그 조회
 */
export async function getErrorAudits(size = 50): Promise<AuditResponse[]> {
  return auditApi.get('/api/v1/admin/audits/errors', { params: { size } })
}

/**
 * 통계 조회
 */
export async function getAuditStats(): Promise<AuditStatsResponse> {
  return auditApi.get('/api/v1/admin/audits/stats')
}

// ============================================
// Helper Functions
// ============================================

export const EVENT_TYPE_LABELS: Record<AuditEventType, string> = {
  LOGIN: '로그인',
  LOGOUT: '로그아웃',
  LOGIN_FAILED: '로그인 실패',
  USER_CREATE: '회원가입',
  USER_UPDATE: '회원정보 수정',
  USER_DELETE: '회원탈퇴',
  JOBPOSTING_CREATE: '공고 등록',
  JOBPOSTING_UPDATE: '공고 수정',
  JOBPOSTING_DELETE: '공고 삭제',
  JOBPOSTING_READ: '공고 조회',
  APPLICATION_CREATE: '지원',
  APPLICATION_CANCEL: '지원 취소',
  APPLICATION_STATUS_CHANGE: '지원 상태 변경',
  RESUME_CREATE: '이력서 등록',
  RESUME_UPDATE: '이력서 수정',
  RESUME_DELETE: '이력서 삭제',
  SCHEDULE_CREATE: '일정 등록',
  SCHEDULE_UPDATE: '일정 수정',
  SCHEDULE_CANCEL: '일정 취소',
  NOTIFICATION_READ: '알림 확인',
  API_REQUEST: 'API 요청',
  SYSTEM_ERROR: '시스템 오류',
  UNKNOWN: '알 수 없음',
}

export function getEventTypeLabel(eventType: AuditEventType): string {
  return EVENT_TYPE_LABELS[eventType] || eventType
}
