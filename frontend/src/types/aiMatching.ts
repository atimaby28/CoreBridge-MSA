// ============================================
// AI Matching Types
// ============================================

// === Request ===

export interface MatchCandidatesRequest {
  jdText: string
  requiredSkills?: string[]
  topK?: number
}

export interface MatchJobpostingsRequest {
  resumeText: string
  topK?: number
}

export interface AiScoreRequest {
  candidateId: string
  jdText: string
  requiredSkills?: string[]
}

export interface SkillGapRequest {
  candidateId: string
  jobpostingId: string
}

// === Response ===

export interface MatchedCandidate {
  candidateId: string
  score: number
  userId?: string
  resumeId?: string
  name?: string
  skills?: string[]
}

export interface MatchCandidatesResponse {
  matches: MatchedCandidate[]
  totalCount: number
}

export interface MatchedJobposting {
  jobpostingId: string
  score: number
  title?: string
}

export interface MatchJobpostingsResponse {
  matches: MatchedJobposting[]
  totalCount: number
}

export interface ScoreDetail {
  skillScore: number
  similarityScore: number
  bonusScore: number
  totalScore: number
  grade: 'A' | 'B' | 'C' | 'D' | 'F'
}

export interface AiScoreResponse {
  candidateId: string
  requiredSkills: string[]
  candidateSkills: string[]
  cosineSimilarity: number
  scoreDetail: ScoreDetail
}

export interface SkillGapResponse {
  candidateId: string
  jobpostingId: string
  candidateSkills: string[]
  requiredSkills: string[]
  matchedSkills: string[]
  missingSkills: string[]
  matchRate: number
  cosineSimilarity: number
}

// === UI Helpers ===

export const GradeColors: Record<string, string> = {
  A: 'bg-green-100 text-green-800 border-green-300',
  B: 'bg-blue-100 text-blue-800 border-blue-300',
  C: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  D: 'bg-orange-100 text-orange-800 border-orange-300',
  F: 'bg-red-100 text-red-800 border-red-300',
}

export const GradeDescriptions: Record<string, string> = {
  A: '매우 적합 (85점 이상)',
  B: '적합 (70~84점)',
  C: '보통 (55~69점)',
  D: '미흡 (40~54점)',
  F: '부적합 (40점 미만)',
}
