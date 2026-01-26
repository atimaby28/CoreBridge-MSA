// 이력서 상태
export type ResumeStatus = 'DRAFT' | 'ANALYZING' | 'ANALYZED' | 'DELETED'

// 상태별 한글명 매핑
export const ResumeStatusNames: Record<ResumeStatus, string> = {
  DRAFT: '작성중',
  ANALYZING: 'AI 분석중',
  ANALYZED: '분석완료',
  DELETED: '삭제됨',
}

// 상태별 색상 매핑
export const ResumeStatusColors: Record<ResumeStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-800',
  ANALYZING: 'bg-yellow-100 text-yellow-800',
  ANALYZED: 'bg-green-100 text-green-800',
  DELETED: 'bg-red-100 text-red-800',
}

// ============================================
// Request DTOs
// ============================================

export interface ResumeUpdateRequest {
  title: string
  content?: string
  memo?: string  // 버전 저장 메모
}

export interface AiResultRequest {
  summary: string
  skills: string  // JSON 배열 문자열
  experienceYears: number
}

// ============================================
// Response DTOs
// ============================================

export interface ResumeResponse {
  resumeId: number
  userId: number
  title: string
  content?: string
  status: ResumeStatus
  currentVersion: number
  createdAt: string
  updatedAt: string
  // AI 분석 결과
  aiSummary?: string
  aiSkills?: string[]
  aiExperienceYears?: number
  analyzedAt?: string
}

export interface VersionResponse {
  versionId: number
  resumeId: number
  version: number
  title: string
  content?: string
  memo?: string
  createdAt: string
}

export interface VersionListResponse {
  versions: VersionResponse[]
  totalCount: number
}
