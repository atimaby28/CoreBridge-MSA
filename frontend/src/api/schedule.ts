import { scheduleApi } from './index'
import type {
  Schedule,
  ScheduleListResponse,
  CalendarEvent,
  CreateScheduleRequest,
  UpdateScheduleRequest,
  UpdateScheduleStatusRequest,
  ConflictCheckResponse
} from '@/types/schedule'

const BASE_URL = '/api/v1/schedules'

// ============================================
// 공통 API
// ============================================

/**
 * 일정 상세 조회
 */
export async function getSchedule(scheduleId: number): Promise<Schedule> {
  return scheduleApi.get(`${BASE_URL}/${scheduleId}`)
}

// ============================================
// 지원자 API
// ============================================

/**
 * 내 일정 목록 조회
 */
export async function getMySchedules(): Promise<ScheduleListResponse> {
  return scheduleApi.get(`${BASE_URL}/my`)
}

/**
 * 내 캘린더 이벤트 조회
 */
export async function getMyCalendarEvents(
  start: string,
  end: string
): Promise<CalendarEvent[]> {
  return scheduleApi.get(`${BASE_URL}/my/calendar`, {
    params: { start, end }
  })
}

// ============================================
// 기업 API
// ============================================

/**
 * 일정 생성
 */
export async function createSchedule(
  request: CreateScheduleRequest
): Promise<Schedule> {
  return scheduleApi.post(BASE_URL, request)
}

/**
 * 일정 수정
 */
export async function updateSchedule(
  scheduleId: number,
  request: UpdateScheduleRequest
): Promise<Schedule> {
  return scheduleApi.put(`${BASE_URL}/${scheduleId}`, request)
}

/**
 * 일정 상태 변경
 */
export async function updateScheduleStatus(
  scheduleId: number,
  request: UpdateScheduleStatusRequest
): Promise<Schedule> {
  return scheduleApi.patch(`${BASE_URL}/${scheduleId}/status`, request)
}

/**
 * 일정 삭제
 */
export async function deleteSchedule(scheduleId: number): Promise<void> {
  return scheduleApi.delete(`${BASE_URL}/${scheduleId}`)
}

/**
 * 메모 수정
 */
export async function updateScheduleMemo(
  scheduleId: number,
  memo: string
): Promise<Schedule> {
  return scheduleApi.patch(`${BASE_URL}/${scheduleId}/memo`, { memo })
}

/**
 * 기업 일정 목록 조회
 */
export async function getCompanySchedules(): Promise<ScheduleListResponse> {
  return scheduleApi.get(`${BASE_URL}/company`)
}

/**
 * 기업 캘린더 이벤트 조회
 */
export async function getCompanyCalendarEvents(
  start: string,
  end: string
): Promise<CalendarEvent[]> {
  return scheduleApi.get(`${BASE_URL}/company/calendar`, {
    params: { start, end }
  })
}

/**
 * 공고별 일정 조회
 */
export async function getSchedulesByJobposting(
  jobpostingId: number
): Promise<Schedule[]> {
  return scheduleApi.get(`${BASE_URL}/jobposting/${jobpostingId}`)
}

/**
 * 지원서별 일정 조회
 */
export async function getSchedulesByApply(applyId: number): Promise<Schedule[]> {
  return scheduleApi.get(`${BASE_URL}/apply/${applyId}`)
}

// ============================================
// 충돌 체크 API
// ============================================

/**
 * 일정 충돌 체크
 */
export async function checkScheduleConflict(params: {
  userId: number
  interviewerId?: number
  startTime: string
  endTime: string
  excludeScheduleId?: number
}): Promise<ConflictCheckResponse> {
  return scheduleApi.get(`${BASE_URL}/check-conflict`, { params })
}
