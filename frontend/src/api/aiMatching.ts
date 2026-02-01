import { applyApi } from './index'
import type {
  MatchCandidatesRequest,
  MatchCandidatesResponse,
  MatchJobpostingsRequest,
  MatchJobpostingsResponse,
  AiScoreRequest,
  AiScoreResponse,
  SkillGapRequest,
  SkillGapResponse,
} from '@/types/aiMatching'

const BASE_URL = '/api/v1/ai-matching'

// ============================================
// 회사용: 후보자 매칭
// ============================================

/** JD에 맞는 후보자 매칭 */
export const matchCandidates = async (request: MatchCandidatesRequest): Promise<MatchCandidatesResponse> => {
  return await applyApi.post(`${BASE_URL}/match`, request)
}

/** 특정 후보자의 상세 스코어 계산 */
export const scoreCandidate = async (request: AiScoreRequest): Promise<AiScoreResponse> => {
  return await applyApi.post(`${BASE_URL}/score`, request)
}

// ============================================
// 구직자용: 채용공고 추천
// ============================================

/** 이력서 기반 채용공고 추천 */
export const matchJobpostings = async (request: MatchJobpostingsRequest): Promise<MatchJobpostingsResponse> => {
  return await applyApi.post(`${BASE_URL}/match-jobpostings`, request)
}

/** 스킬 갭 분석 */
export const analyzeSkillGap = async (request: SkillGapRequest): Promise<SkillGapResponse> => {
  return await applyApi.post(`${BASE_URL}/skill-gap`, request)
}
