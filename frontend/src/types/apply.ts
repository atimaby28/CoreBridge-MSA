// 채용 프로세스 단계
export type ProcessStep =
  | 'APPLIED'
  | 'DOCUMENT_REVIEW'
  | 'DOCUMENT_PASS'
  | 'DOCUMENT_FAIL'
  | 'CODING_TEST'
  | 'CODING_PASS'
  | 'CODING_FAIL'
  | 'INTERVIEW_1'
  | 'INTERVIEW_1_PASS'
  | 'INTERVIEW_1_FAIL'
  | 'INTERVIEW_2'
  | 'INTERVIEW_2_PASS'
  | 'INTERVIEW_2_FAIL'
  | 'FINAL_REVIEW'
  | 'FINAL_PASS'
  | 'FINAL_FAIL'

// 단계별 한글명 매핑 (Backend ProcessStep.displayName과 일치)
export const ProcessStepNames: Record<ProcessStep, string> = {
  APPLIED: '지원완료',
  DOCUMENT_REVIEW: '서류검토중',
  DOCUMENT_PASS: '서류합격',
  DOCUMENT_FAIL: '서류탈락',
  CODING_TEST: '코딩테스트',
  CODING_PASS: '코딩테스트합격',
  CODING_FAIL: '코딩테스트탈락',
  INTERVIEW_1: '1차면접',
  INTERVIEW_1_PASS: '1차면접합격',
  INTERVIEW_1_FAIL: '1차면접탈락',
  INTERVIEW_2: '2차면접',
  INTERVIEW_2_PASS: '2차면접합격',
  INTERVIEW_2_FAIL: '2차면접탈락',
  FINAL_REVIEW: '최종검토',
  FINAL_PASS: '최종합격',
  FINAL_FAIL: '최종불합격',
}

// 단계별 색상 매핑
export const ProcessStepColors: Record<ProcessStep, string> = {
  APPLIED: 'bg-gray-100 text-gray-800',
  DOCUMENT_REVIEW: 'bg-blue-100 text-blue-800',
  DOCUMENT_PASS: 'bg-green-100 text-green-800',
  DOCUMENT_FAIL: 'bg-red-100 text-red-800',
  CODING_TEST: 'bg-purple-100 text-purple-800',
  CODING_PASS: 'bg-green-100 text-green-800',
  CODING_FAIL: 'bg-red-100 text-red-800',
  INTERVIEW_1: 'bg-yellow-100 text-yellow-800',
  INTERVIEW_1_PASS: 'bg-green-100 text-green-800',
  INTERVIEW_1_FAIL: 'bg-red-100 text-red-800',
  INTERVIEW_2: 'bg-orange-100 text-orange-800',
  INTERVIEW_2_PASS: 'bg-green-100 text-green-800',
  INTERVIEW_2_FAIL: 'bg-red-100 text-red-800',
  FINAL_REVIEW: 'bg-indigo-100 text-indigo-800',
  FINAL_PASS: 'bg-emerald-100 text-emerald-800',
  FINAL_FAIL: 'bg-red-100 text-red-800',
}

// ============================================
// Request DTOs
// ============================================

export interface ApplyCreateRequest {
  jobpostingId: number
  userId: number
  resumeId?: number
  coverLetter?: string
}

export interface UpdateMemoRequest {
  memo: string
}

export interface TransitionRequest {
  nextStep: ProcessStep
  changedBy?: number
  reason?: string
  note?: string
}

// ============================================
// Response DTOs
// ============================================

export interface ApplyResponse {
  applyId: number
  jobpostingId: number
  userId: number
  resumeId?: number
  coverLetter?: string
  memo?: string
  createdAt: string
  updatedAt: string
}

export interface ApplyDetailResponse {
  applyId: number
  jobpostingId: number
  userId: number
  resumeId?: number
  coverLetter?: string
  memo?: string
  appliedAt: string
  // Process 정보
  processId: number
  currentStep: ProcessStep
  currentStepName: string
  previousStep?: ProcessStep
  previousStepName?: string
  allowedNextSteps: string[]
  completed: boolean
  passed: boolean
  failed: boolean
  stepChangedAt: string
}

export interface ApplyPageResponse {
  applies: ApplyDetailResponse[]
  totalCount: number
}

export interface ProcessResponse {
  processId: number
  applyId: number
  jobpostingId: number
  userId: number
  currentStep: ProcessStep
  currentStepName: string
  previousStep?: ProcessStep
  previousStepName?: string
  allowedNextSteps: string[]
  completed: boolean
  passed: boolean
  failed: boolean
  stepChangedAt: string
  createdAt: string
  updatedAt: string
}

export interface ProcessPageResponse {
  processes: ProcessResponse[]
  processCount: number
}

export interface HistoryResponse {
  historyId: number
  processId: number
  applyId: number
  fromStep?: ProcessStep
  fromStepName?: string
  toStep: ProcessStep
  toStepName: string
  changedBy?: number
  reason?: string
  note?: string
  createdAt: string
}

export interface StepInfoResponse {
  step: ProcessStep
  displayName: string
  allowedNextSteps: string[]
  terminal: boolean
}

export interface UserStatsResponse {
  totalProcesses: number
  pendingProcesses: number
  passedProcesses: number
  failedProcesses: number
  passRate: number
}

export interface CompanyStatsResponse {
  totalApplicants: number
  pendingApplicants: number
  interviewingApplicants: number
  passedApplicants: number
  failedApplicants: number
  passRate: number
}
