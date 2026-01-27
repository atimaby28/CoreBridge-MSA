import { applyApi } from './index'
import type {
  AiMatchRequest,
  AiMatchResponse,
  AiScoreRequest,
  AiScoreResponse,
} from '@/types/aiMatching'

const BASE_URL = '/api/v1/ai-matching'

/**
 * JD에 맞는 후보자 매칭
 */
export const matchCandidates = async (request: AiMatchRequest): Promise<AiMatchResponse> => {
  return await applyApi.post(`${BASE_URL}/match`, request)
}

/**
 * 특정 후보자의 상세 스코어 계산
 */
export const scoreCandidate = async (request: AiScoreRequest): Promise<AiScoreResponse> => {
  return await applyApi.post(`${BASE_URL}/score`, request)
}

/**
 * 채용공고 ID로 후보자 매칭
 */
export const matchByJobposting = async (jobpostingId: number, topK: number = 10): Promise<AiMatchResponse> => {
  return await applyApi.get(`${BASE_URL}/jobposting/${jobpostingId}?topK=${topK}`)
}
