// ============================================
// Schedule Types (일정)
// ============================================

export type ScheduleType = 
  | 'CODING_TEST'
  | 'INTERVIEW_1'
  | 'INTERVIEW_2'
  | 'FINAL_INTERVIEW'
  | 'ORIENTATION'
  | 'OTHER'

export type ScheduleStatus = 
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export interface Schedule {
  id: number
  applyId: number
  jobpostingId: number
  userId: number
  companyId: number
  type: ScheduleType
  typeDescription: string
  title: string
  description?: string
  location?: string
  startTime: string
  endTime: string
  interviewerId?: number
  interviewerName?: string
  status: ScheduleStatus
  statusDescription: string
  memo?: string
  createdAt: string
  updatedAt: string
}

export interface ScheduleListResponse {
  schedules: Schedule[]
  totalCount: number
  upcomingCount: number
  completedCount: number
}

// ============================================
// Calendar Event (FullCalendar 형식)
// ============================================

export interface CalendarEvent {
  id: string
  title: string
  start: string
  end: string
  color: string
  backgroundColor: string
  borderColor: string
  textColor: string
  allDay: boolean
  extendedProps: {
    scheduleId: number
    applyId: number
    jobpostingId: number
    userId: number
    type: ScheduleType
    typeDescription: string
    status: ScheduleStatus
    statusDescription: string
    location?: string
    description?: string
    interviewerId?: number
    interviewerName?: string
  }
}

// ============================================
// Request DTOs
// ============================================

export interface CreateScheduleRequest {
  applyId: number
  jobpostingId: number
  userId: number
  type: ScheduleType
  title: string
  description?: string
  location?: string
  startTime: string
  endTime: string
  interviewerId?: number
  interviewerName?: string
}

export interface UpdateScheduleRequest {
  title: string
  description?: string
  location?: string
  startTime: string
  endTime: string
  interviewerId?: number
  interviewerName?: string
}

export interface UpdateScheduleStatusRequest {
  status: ScheduleStatus
}

// ============================================
// Conflict Check
// ============================================

export interface ConflictDetail {
  type: 'APPLICANT' | 'INTERVIEWER'
  scheduleId: number
  title: string
  startTime: string
  endTime: string
  message: string
}

export interface ConflictCheckResponse {
  hasConflict: boolean
  conflicts: ConflictDetail[]
}

// ============================================
// Helper Constants
// ============================================

export const SCHEDULE_TYPE_LABELS: Record<ScheduleType, string> = {
  CODING_TEST: '코딩 테스트',
  INTERVIEW_1: '1차 면접',
  INTERVIEW_2: '2차 면접',
  FINAL_INTERVIEW: '최종 면접',
  ORIENTATION: '오리엔테이션',
  OTHER: '기타'
}

export const SCHEDULE_STATUS_LABELS: Record<ScheduleStatus, string> = {
  SCHEDULED: '예정',
  IN_PROGRESS: '진행 중',
  COMPLETED: '완료',
  CANCELLED: '취소',
  NO_SHOW: '불참'
}

export const SCHEDULE_TYPE_COLORS: Record<ScheduleType, string> = {
  CODING_TEST: '#8B5CF6',      // 보라색
  INTERVIEW_1: '#3B82F6',      // 파란색
  INTERVIEW_2: '#10B981',      // 초록색
  FINAL_INTERVIEW: '#F59E0B',  // 주황색
  ORIENTATION: '#EC4899',      // 핑크색
  OTHER: '#6B7280'             // 회색
}

export const SCHEDULE_TYPE_ICONS: Record<ScheduleType, string> = {
  CODING_TEST: '💻',
  INTERVIEW_1: '👔',
  INTERVIEW_2: '🎯',
  FINAL_INTERVIEW: '⭐',
  ORIENTATION: '📋',
  OTHER: '📅'
}
