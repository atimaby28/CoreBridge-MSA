import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  Schedule,
  CalendarEvent,
  CreateScheduleRequest,
  UpdateScheduleRequest,
  ScheduleStatus
} from '@/types/schedule'
import * as scheduleApi from '@/api/schedule'

export const useScheduleStore = defineStore('schedule', () => {
  // ============================================
  // State
  // ============================================
  const schedules = ref<Schedule[]>([])
  const calendarEvents = ref<CalendarEvent[]>([])
  const selectedSchedule = ref<Schedule | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const upcomingCount = ref(0)
  const completedCount = ref(0)

  // ============================================
  // Getters
  // ============================================
  const upcomingSchedules = computed(() =>
    schedules.value.filter(s => s.status === 'SCHEDULED')
  )

  const pastSchedules = computed(() =>
    schedules.value.filter(s => s.status === 'COMPLETED' || s.status === 'CANCELLED')
  )

  // ============================================
  // Actions
  // ============================================

  /**
   * 내 일정 목록 조회 (지원자)
   */
  async function fetchMySchedules() {
    try {
      loading.value = true
      error.value = null
      const response = await scheduleApi.getMySchedules()
      schedules.value = response.schedules
      upcomingCount.value = response.upcomingCount
      completedCount.value = response.completedCount
    } catch (e: any) {
      console.error('내 일정 조회 실패:', e)
      error.value = e.message || '일정을 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  /**
   * 기업 일정 목록 조회
   */
  async function fetchCompanySchedules() {
    try {
      loading.value = true
      error.value = null
      const response = await scheduleApi.getCompanySchedules()
      schedules.value = response.schedules
      upcomingCount.value = response.upcomingCount
      completedCount.value = response.completedCount
    } catch (e: any) {
      console.error('기업 일정 조회 실패:', e)
      error.value = e.message || '일정을 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  /**
   * 캘린더 이벤트 조회 (지원자)
   */
  async function fetchMyCalendarEvents(start: string, end: string) {
    try {
      loading.value = true
      calendarEvents.value = await scheduleApi.getMyCalendarEvents(start, end)
    } catch (e: any) {
      console.error('캘린더 이벤트 조회 실패:', e)
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  /**
   * 캘린더 이벤트 조회 (기업)
   */
  async function fetchCompanyCalendarEvents(start: string, end: string) {
    try {
      loading.value = true
      calendarEvents.value = await scheduleApi.getCompanyCalendarEvents(start, end)
    } catch (e: any) {
      console.error('캘린더 이벤트 조회 실패:', e)
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  /**
   * 일정 상세 조회
   */
  async function fetchSchedule(scheduleId: number) {
    try {
      loading.value = true
      selectedSchedule.value = await scheduleApi.getSchedule(scheduleId)
    } catch (e: any) {
      console.error('일정 상세 조회 실패:', e)
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  /**
   * 일정 생성 (기업)
   */
  async function createSchedule(request: CreateScheduleRequest) {
    try {
      loading.value = true
      const newSchedule = await scheduleApi.createSchedule(request)
      schedules.value.unshift(newSchedule)
      upcomingCount.value++
      return newSchedule
    } catch (e: any) {
      console.error('일정 생성 실패:', e)
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 일정 수정 (기업)
   */
  async function updateSchedule(scheduleId: number, request: UpdateScheduleRequest) {
    try {
      loading.value = true
      const updated = await scheduleApi.updateSchedule(scheduleId, request)
      const index = schedules.value.findIndex(s => s.id === scheduleId)
      if (index !== -1) {
        schedules.value[index] = updated
      }
      if (selectedSchedule.value?.id === scheduleId) {
        selectedSchedule.value = updated
      }
      return updated
    } catch (e: any) {
      console.error('일정 수정 실패:', e)
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 일정 상태 변경 (기업)
   */
  async function updateStatus(scheduleId: number, status: ScheduleStatus) {
    try {
      loading.value = true
      const updated = await scheduleApi.updateScheduleStatus(scheduleId, { status })
      const index = schedules.value.findIndex(s => s.id === scheduleId)
      if (index !== -1) {
        schedules.value[index] = updated
      }
      if (selectedSchedule.value?.id === scheduleId) {
        selectedSchedule.value = updated
      }
      return updated
    } catch (e: any) {
      console.error('일정 상태 변경 실패:', e)
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 일정 삭제 (기업)
   */
  async function deleteSchedule(scheduleId: number) {
    try {
      loading.value = true
      await scheduleApi.deleteSchedule(scheduleId)
      schedules.value = schedules.value.filter(s => s.id !== scheduleId)
      if (selectedSchedule.value?.id === scheduleId) {
        selectedSchedule.value = null
      }
    } catch (e: any) {
      console.error('일정 삭제 실패:', e)
      error.value = e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 충돌 체크
   */
  async function checkConflict(params: {
    userId: number
    interviewerId?: number
    startTime: string
    endTime: string
    excludeScheduleId?: number
  }) {
    try {
      return await scheduleApi.checkScheduleConflict(params)
    } catch (e: any) {
      console.error('충돌 체크 실패:', e)
      throw e
    }
  }

  /**
   * 선택된 일정 초기화
   */
  function clearSelectedSchedule() {
    selectedSchedule.value = null
  }

  return {
    // State
    schedules,
    calendarEvents,
    selectedSchedule,
    loading,
    error,
    upcomingCount,
    completedCount,

    // Getters
    upcomingSchedules,
    pastSchedules,

    // Actions
    fetchMySchedules,
    fetchCompanySchedules,
    fetchMyCalendarEvents,
    fetchCompanyCalendarEvents,
    fetchSchedule,
    createSchedule,
    updateSchedule,
    updateStatus,
    deleteSchedule,
    checkConflict,
    clearSelectedSchedule
  }
})
