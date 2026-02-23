import { applyApi } from './index'
import type {
  ApplyCreateRequest,
  ApplyDetailResponse,
  ApplyPageResponse,
  UpdateMemoRequest,
  TransitionRequest,
  ProcessResponse,
  ProcessPageResponse,
  HistoryResponse,
  StepInfoResponse,
  UserStatsResponse,
  CompanyStatsResponse,
  ProcessStep,
} from '@/types/apply'

const APPLY_BASE = '/api/v1/applies'
const PROCESS_BASE = '/api/v1/processes'

// ============================================
// Apply API
// ============================================

/**
 * 지원하기
 */
export const createApply = async (request: ApplyCreateRequest): Promise<ApplyDetailResponse> => {
  return await applyApi.post(APPLY_BASE, request)
}

/**
 * 지원 취소
 */
export const cancelApply = async (applyId: number, userId: number): Promise<void> => {
  await applyApi.delete(`${APPLY_BASE}/${applyId}/users/${userId}`)
}

/**
 * 지원 상세 조회
 */
export const getApply = async (applyId: number): Promise<ApplyDetailResponse> => {
  return await applyApi.get(`${APPLY_BASE}/${applyId}`)
}

/**
 * 내 지원 목록 조회
 */
export const getMyApplies = async (userId: number): Promise<ApplyPageResponse> => {
  return await applyApi.get(`${APPLY_BASE}/users/${userId}`)
}

/**
 * 공고별 지원자 목록 조회
 */
export const getAppliesByJobposting = async (jobpostingId: number): Promise<ApplyPageResponse> => {
  return await applyApi.get(`${APPLY_BASE}/jobpostings/${jobpostingId}`)
}

/**
 * 공고별 특정 단계 지원자 목록 조회
 */
export const getAppliesByStep = async (
  jobpostingId: number,
  step: ProcessStep
): Promise<ApplyPageResponse> => {
  return await applyApi.get(`${APPLY_BASE}/jobpostings/${jobpostingId}/steps/${step}`)
}

/**
 * 메모 수정
 */
export const updateMemo = async (
  applyId: number,
  request: UpdateMemoRequest
): Promise<ApplyDetailResponse> => {
  return await applyApi.patch(`${APPLY_BASE}/${applyId}/memo`, request)
}

/**
 * 지원 이력 조회
 */
export const getApplyHistory = async (applyId: number): Promise<HistoryResponse[]> => {
  return await applyApi.get(`${APPLY_BASE}/${applyId}/history`)
}

/**
 * 사용자별 지원 통계
 */
export const getUserApplyStats = async (userId: number): Promise<UserStatsResponse> => {
  return await applyApi.get(`${APPLY_BASE}/users/${userId}/stats`)
}

/**
 * 공고별 지원자 통계
 */
export const getJobpostingApplyStats = async (
  jobpostingId: number
): Promise<CompanyStatsResponse> => {
  return await applyApi.get(`${APPLY_BASE}/jobpostings/${jobpostingId}/stats`)
}

/**
 * 기업 전체 통계
 */
export const getCompanyApplyStats = async (
  jobpostingIds: number[]
): Promise<CompanyStatsResponse> => {
  return await applyApi.post(`${APPLY_BASE}/company/stats`, jobpostingIds)
}

// ============================================
// Process API (State Machine)
// ============================================

/**
 * 상태 전이
 */
export const transitionProcess = async (
  processId: number,
  request: TransitionRequest
): Promise<ProcessResponse> => {
  return await applyApi.patch(`${PROCESS_BASE}/${processId}/transition`, request)
}

/**
 * 지원 ID로 상태 전이
 */
export const transitionByApplyId = async (
  applyId: number,
  request: TransitionRequest
): Promise<ProcessResponse> => {
  return await applyApi.patch(`${PROCESS_BASE}/applies/${applyId}/transition`, request)
}

/**
 * 프로세스 상세 조회
 */
export const getProcess = async (processId: number): Promise<ProcessResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/${processId}`)
}

/**
 * 지원 ID로 프로세스 조회
 */
export const getProcessByApplyId = async (applyId: number): Promise<ProcessResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/applies/${applyId}`)
}

/**
 * 공고별 프로세스 목록
 */
export const getProcessesByJobposting = async (
  jobpostingId: number
): Promise<ProcessPageResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/jobpostings/${jobpostingId}`)
}

/**
 * 공고별 특정 단계 프로세스 목록
 */
export const getProcessesByStep = async (
  jobpostingId: number,
  step: ProcessStep
): Promise<ProcessPageResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/jobpostings/${jobpostingId}/steps/${step}`)
}

/**
 * 사용자별 프로세스 목록
 */
export const getProcessesByUser = async (userId: number): Promise<ProcessPageResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/users/${userId}`)
}

/**
 * 프로세스 이력 조회
 */
export const getProcessHistory = async (processId: number): Promise<HistoryResponse[]> => {
  return await applyApi.get(`${PROCESS_BASE}/${processId}/history`)
}

/**
 * 지원 ID로 프로세스 이력 조회
 */
export const getProcessHistoryByApplyId = async (applyId: number): Promise<HistoryResponse[]> => {
  return await applyApi.get(`${PROCESS_BASE}/applies/${applyId}/history`)
}

/**
 * 모든 단계 정보 조회 (프론트엔드 UI용)
 */
export const getAllSteps = async (): Promise<StepInfoResponse[]> => {
  return await applyApi.get(`${PROCESS_BASE}/steps`)
}

/**
 * 사용자 통계
 */
export const getUserProcessStats = async (userId: number): Promise<UserStatsResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/users/${userId}/stats`)
}

/**
 * 공고별 통계
 */
export const getJobpostingProcessStats = async (
  jobpostingId: number
): Promise<CompanyStatsResponse> => {
  return await applyApi.get(`${PROCESS_BASE}/jobpostings/${jobpostingId}/stats`)
}

/**
 * 기업 전체 통계
 */
export const getCompanyProcessStats = async (
  jobpostingIds: number[]
): Promise<CompanyStatsResponse> => {
  return await applyApi.post(`${PROCESS_BASE}/company/stats`, jobpostingIds)
}