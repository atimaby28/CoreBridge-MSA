import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ResumeResponse, VersionResponse, ResumeUpdateRequest } from '@/types/resume'
import {
  getMyResume,
  updateMyResume,
  getVersions,
  getVersion,
  restoreVersion,
  requestAnalysis,
} from '@/api/resume'

export const useResumeStore = defineStore('resume', () => {
  // ============================================
  // State
  // ============================================
  const resume = ref<ResumeResponse | null>(null)
  const versions = ref<VersionResponse[]>([])
  const selectedVersion = ref<VersionResponse | null>(null)
  const loading = ref(false)
  const analyzing = ref(false)
  const error = ref<string | null>(null)

  // ============================================
  // Getters
  // ============================================
  const hasResume = computed(() => resume.value !== null)
  const isAnalyzing = computed(() => resume.value?.status === 'ANALYZING')
  const isAnalyzed = computed(() => resume.value?.status === 'ANALYZED')
  const hasAiResult = computed(() => !!resume.value?.aiSummary)

  // ============================================
  // Actions
  // ============================================

  /**
   * 내 이력서 조회 (없으면 자동 생성)
   */
  async function fetchResume(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      resume.value = await getMyResume()
    } catch (e) {
      error.value = e instanceof Error ? e.message : '이력서 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 이력서 업데이트
   */
  async function update(request: ResumeUpdateRequest): Promise<void> {
    loading.value = true
    error.value = null
    try {
      resume.value = await updateMyResume(request)
      // 버전 목록 갱신
      await fetchVersions()
    } catch (e) {
      error.value = e instanceof Error ? e.message : '이력서 저장 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 버전 목록 조회
   */
  async function fetchVersions(): Promise<void> {
    try {
      const response = await getVersions()
      versions.value = response.versions
    } catch (e) {
      console.error('버전 목록 조회 실패:', e)
    }
  }

  /**
   * 특정 버전 조회
   */
  async function fetchVersion(version: number): Promise<void> {
    try {
      selectedVersion.value = await getVersion(version)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '버전 조회 실패'
      throw e
    }
  }

  /**
   * 특정 버전으로 복원
   */
  async function restore(version: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      resume.value = await restoreVersion(version)
      await fetchVersions()
    } catch (e) {
      error.value = e instanceof Error ? e.message : '버전 복원 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * AI 분석 요청
   */
  async function analyze(): Promise<void> {
    analyzing.value = true
    error.value = null
    try {
      resume.value = await requestAnalysis()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 분석 요청 실패'
      throw e
    } finally {
      analyzing.value = false
    }
  }

  /**
   * 상태 초기화
   */
  function reset(): void {
    resume.value = null
    versions.value = []
    selectedVersion.value = null
    loading.value = false
    analyzing.value = false
    error.value = null
  }

  return {
    // State
    resume,
    versions,
    selectedVersion,
    loading,
    analyzing,
    error,
    // Getters
    hasResume,
    isAnalyzing,
    isAnalyzed,
    hasAiResult,
    // Actions
    fetchResume,
    update,
    fetchVersions,
    fetchVersion,
    restore,
    analyze,
    reset,
  }
})
