import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  ApplyDetailResponse,
  ApplyPageResponse,
  ProcessResponse,
  HistoryResponse,
  StepInfoResponse,
  UserStatsResponse,
  CompanyStatsResponse,
  ProcessStep,
  ApplyCreateRequest,
  TransitionRequest,
} from '@/types/apply'
import * as applyApi from '@/api/apply'

export const useApplyStore = defineStore('apply', () => {
  // ============================================
  // State
  // ============================================
  const myApplies = ref<ApplyDetailResponse[]>([])
  const currentApply = ref<ApplyDetailResponse | null>(null)
  const jobpostingApplies = ref<ApplyDetailResponse[]>([])
  const processHistory = ref<HistoryResponse[]>([])
  const allSteps = ref<StepInfoResponse[]>([])
  const userStats = ref<UserStatsResponse | null>(null)
  const companyStats = ref<CompanyStatsResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ============================================
  // Getters
  // ============================================
  const myAppliesCount = computed(() => myApplies.value.length)

  const pendingApplies = computed(() =>
    myApplies.value.filter((apply) => !apply.completed)
  )

  const completedApplies = computed(() =>
    myApplies.value.filter((apply) => apply.completed)
  )

  const passedApplies = computed(() =>
    myApplies.value.filter((apply) => apply.passed)
  )

  const failedApplies = computed(() =>
    myApplies.value.filter((apply) => apply.failed)
  )

  // ============================================
  // Actions - Apply
  // ============================================

  /**
   * 지원하기
   */
  async function createApply(request: ApplyCreateRequest): Promise<ApplyDetailResponse> {
    loading.value = true
    error.value = null
    try {
      const result = await applyApi.createApply(request)
      myApplies.value.unshift(result)
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || '지원에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 지원 취소
   */
  async function cancelApply(applyId: number, userId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      await applyApi.cancelApply(applyId, userId)
      myApplies.value = myApplies.value.filter((apply) => apply.applyId !== applyId)
    } catch (err: any) {
      error.value = err.response?.data?.message || '지원 취소에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 지원 상세 조회
   */
  async function fetchApply(applyId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentApply.value = await applyApi.getApply(applyId)
    } catch (err: any) {
      error.value = err.response?.data?.message || '지원 정보를 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 내 지원 목록 조회
   */
  async function fetchMyApplies(userId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const result = await applyApi.getMyApplies(userId)
      myApplies.value = result.applies
    } catch (err: any) {
      error.value = err.response?.data?.message || '지원 목록을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 공고별 지원자 목록 조회
   */
  async function fetchAppliesByJobposting(jobpostingId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const result = await applyApi.getAppliesByJobposting(jobpostingId)
      jobpostingApplies.value = result.applies
    } catch (err: any) {
      error.value = err.response?.data?.message || '지원자 목록을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 공고별 특정 단계 지원자 목록 조회
   */
  async function fetchAppliesByStep(jobpostingId: number, step: ProcessStep): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const result = await applyApi.getAppliesByStep(jobpostingId, step)
      jobpostingApplies.value = result.applies
    } catch (err: any) {
      error.value = err.response?.data?.message || '지원자 목록을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  // ============================================
  // Actions - Process (State Machine)
  // ============================================

  /**
   * 상태 전이
   */
  async function transitionProcess(
    processId: number,
    request: TransitionRequest
  ): Promise<ProcessResponse> {
    loading.value = true
    error.value = null
    try {
      const result = await applyApi.transitionProcess(processId, request)
      // 현재 조회 중인 지원 정보 업데이트
      if (currentApply.value && currentApply.value.processId === processId) {
        currentApply.value.currentStep = result.currentStep
        currentApply.value.currentStepName = result.currentStepName
        currentApply.value.previousStep = result.previousStep
        currentApply.value.previousStepName = result.previousStepName
        currentApply.value.allowedNextSteps = result.allowedNextSteps
        currentApply.value.completed = result.completed
        currentApply.value.passed = result.passed
        currentApply.value.failed = result.failed
      }
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || '상태 전이에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 지원 ID로 상태 전이
   */
  async function transitionByApplyId(
    applyId: number,
    request: TransitionRequest
  ): Promise<ProcessResponse> {
    loading.value = true
    error.value = null
    try {
      const result = await applyApi.transitionByApplyId(applyId, request)
      // 목록 업데이트
      const index = jobpostingApplies.value.findIndex((a) => a.applyId === applyId)
      if (index !== -1) {
        jobpostingApplies.value[index].currentStep = result.currentStep
        jobpostingApplies.value[index].currentStepName = result.currentStepName
        jobpostingApplies.value[index].completed = result.completed
        jobpostingApplies.value[index].passed = result.passed
        jobpostingApplies.value[index].failed = result.failed
      }
      return result
    } catch (err: any) {
      error.value = err.response?.data?.message || '상태 전이에 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 프로세스 이력 조회
   */
  async function fetchProcessHistory(processId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      processHistory.value = await applyApi.getProcessHistory(processId)
    } catch (err: any) {
      error.value = err.response?.data?.message || '이력을 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 모든 단계 정보 조회
   */
  async function fetchAllSteps(): Promise<void> {
    try {
      allSteps.value = await applyApi.getAllSteps()
    } catch (err: any) {
      console.error('단계 정보 조회 실패:', err)
    }
  }

  // ============================================
  // Actions - Statistics
  // ============================================

  /**
   * 사용자 통계 조회
   */
  async function fetchUserStats(userId: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      userStats.value = await applyApi.getUserProcessStats(userId)
    } catch (err: any) {
      error.value = err.response?.data?.message || '통계를 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 기업 통계 조회
   */
  async function fetchCompanyStats(jobpostingIds: number[]): Promise<void> {
    loading.value = true
    error.value = null
    try {
      companyStats.value = await applyApi.getCompanyProcessStats(jobpostingIds)
    } catch (err: any) {
      error.value = err.response?.data?.message || '통계를 불러오는데 실패했습니다.'
      throw err
    } finally {
      loading.value = false
    }
  }

  // ============================================
  // Utility
  // ============================================

  function clearError(): void {
    error.value = null
  }

  function clearCurrentApply(): void {
    currentApply.value = null
  }

  function $reset(): void {
    myApplies.value = []
    currentApply.value = null
    jobpostingApplies.value = []
    processHistory.value = []
    userStats.value = null
    companyStats.value = null
    loading.value = false
    error.value = null
  }

  return {
    // State
    myApplies,
    currentApply,
    jobpostingApplies,
    processHistory,
    allSteps,
    userStats,
    companyStats,
    loading,
    error,

    // Getters
    myAppliesCount,
    pendingApplies,
    completedApplies,
    passedApplies,
    failedApplies,

    // Actions - Apply
    createApply,
    cancelApply,
    fetchApply,
    fetchMyApplies,
    fetchAppliesByJobposting,
    fetchAppliesByStep,

    // Actions - Process
    transitionProcess,
    transitionByApplyId,
    fetchProcessHistory,
    fetchAllSteps,

    // Actions - Statistics
    fetchUserStats,
    fetchCompanyStats,

    // Utility
    clearError,
    clearCurrentApply,
    $reset,
  }
})
