import { resumeApi } from './index'
import type {
  ResumeUpdateRequest,
  ResumeResponse,
  VersionResponse,
  VersionListResponse,
} from '@/types/resume'

const BASE_URL = '/api/v1/resumes'

// ============================================
// Resume API
// ============================================

/**
 * 내 이력서 조회 (없으면 자동 생성)
 */
export const getMyResume = async (): Promise<ResumeResponse> => {
  return await resumeApi.get(`${BASE_URL}/me`)
}

/**
 * 내 이력서 업데이트
 */
export const updateMyResume = async (request: ResumeUpdateRequest): Promise<ResumeResponse> => {
  return await resumeApi.put(`${BASE_URL}/me`, request)
}

// ============================================
// Version API
// ============================================

/**
 * 버전 목록 조회
 */
export const getVersions = async (): Promise<VersionListResponse> => {
  return await resumeApi.get(`${BASE_URL}/me/versions`)
}

/**
 * 특정 버전 조회
 */
export const getVersion = async (version: number): Promise<VersionResponse> => {
  return await resumeApi.get(`${BASE_URL}/me/versions/${version}`)
}

/**
 * 특정 버전으로 복원
 */
export const restoreVersion = async (version: number): Promise<ResumeResponse> => {
  return await resumeApi.post(`${BASE_URL}/me/versions/${version}/restore`)
}

// ============================================
// AI Analysis API
// ============================================

/**
 * AI 분석 요청
 */
export const requestAnalysis = async (): Promise<ResumeResponse> => {
  return await resumeApi.post(`${BASE_URL}/me/analyze`)
}
