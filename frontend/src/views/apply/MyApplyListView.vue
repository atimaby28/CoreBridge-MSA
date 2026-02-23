<template>
  <div class="max-w-6xl mx-auto px-4 py-8">
    <!-- 헤더 -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">내 지원 현황</h1>
      <p class="mt-2 text-gray-600">지원한 채용공고의 진행 상태를 확인하세요.</p>
    </div>

    <!-- 통계 카드 -->
    <div v-if="userStats" class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-sm text-gray-500">총 지원</p>
        <p class="text-2xl font-bold text-gray-900">{{ userStats.totalProcesses }}</p>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-sm text-gray-500">진행 중</p>
        <p class="text-2xl font-bold text-blue-600">{{ userStats.pendingProcesses }}</p>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-sm text-gray-500">합격</p>
        <p class="text-2xl font-bold text-green-600">{{ userStats.passedProcesses }}</p>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-sm text-gray-500">불합격</p>
        <p class="text-2xl font-bold text-red-600">{{ userStats.failedProcesses }}</p>
      </div>
    </div>

    <!-- 필터 탭 -->
    <div class="flex gap-2 mb-6">
      <button
        v-for="filter in filters"
        :key="filter.value"
        @click="activeFilter = filter.value"
        :class="[
          'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
          activeFilter === filter.value
            ? 'bg-blue-600 text-white'
            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
        ]"
      >
        {{ filter.label }}
      </button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">불러오는 중...</p>
    </div>

    <!-- 에러 -->
    <div v-else-if="error" class="text-center py-12">
      <p class="text-red-500">{{ error }}</p>
      <button @click="fetchData" class="mt-4 text-blue-600 hover:underline">다시 시도</button>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="filteredApplies.length === 0" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <p class="mt-4 text-gray-500">지원 내역이 없습니다.</p>
      <router-link to="/jobpostings" class="mt-4 inline-block text-blue-600 hover:underline">
        채용공고 보러가기
      </router-link>
    </div>

    <!-- 지원 목록 -->
    <div v-else class="space-y-4">
      <div
        v-for="apply in filteredApplies"
        :key="apply.applyId"
        class="bg-white rounded-lg shadow hover:shadow-md transition-shadow p-6"
      >
        <div class="flex justify-between items-start">
          <div class="flex-1">
            <!-- 공고 정보 (추후 공고 API 연동) -->
            <h3 class="text-lg font-semibold text-gray-900">
              채용공고 #{{ apply.jobpostingId }}
            </h3>
            <p class="text-sm text-gray-500 mt-1">
              지원일: {{ formatDate(apply.appliedAt) }}
            </p>
          </div>
          
          <!-- 상태 뱃지 -->
          <span :class="['px-3 py-1 rounded-full text-sm font-medium', getStepColor(apply.currentStep)]">
            {{ apply.currentStepName }}
          </span>
        </div>

        <!-- 프로세스 타임라인 -->
        <div class="mt-4 pt-4 border-t border-gray-100">
          <div class="flex items-center gap-2 text-sm">
            <span class="text-gray-500">진행 상태:</span>
            <span v-if="apply.completed && apply.passed" class="text-green-600 font-medium">
              ✅ 최종 합격
            </span>
            <span v-else-if="apply.completed && apply.failed" class="text-red-600 font-medium">
              ❌ 불합격
            </span>
            <span v-else class="text-blue-600 font-medium">
              🔄 진행 중
            </span>
          </div>
          
          <!-- 다음 가능한 단계 -->
          <div v-if="!apply.completed && apply.allowedNextSteps.length > 0" class="mt-2">
            <span class="text-xs text-gray-400">
              다음 단계: {{ apply.allowedNextSteps.map(s => getStepName(s)).join(', ') }}
            </span>
          </div>
        </div>

        <!-- 액션 버튼 -->
        <div class="mt-4 flex gap-2">
          <button
            @click="viewHistory(apply)"
            class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded"
          >
            이력 보기
          </button>
          <button
            v-if="apply.currentStep === 'APPLIED'"
            @click="handleCancel(apply)"
            class="px-3 py-1.5 text-sm text-red-600 hover:text-red-700 hover:bg-red-50 rounded"
          >
            지원 취소
          </button>
        </div>
      </div>
    </div>

    <!-- 이력 모달 -->
    <div v-if="showHistoryModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 max-h-[80vh] overflow-hidden">
        <div class="p-4 border-b flex justify-between items-center">
          <h3 class="font-semibold">진행 이력</h3>
          <button @click="showHistoryModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="p-4 overflow-y-auto max-h-96">
          <div v-if="historyLoading" class="text-center py-4">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mx-auto"></div>
          </div>
          <div v-else-if="processHistory.length === 0" class="text-center py-4 text-gray-500">
            이력이 없습니다.
          </div>
          <div v-else class="space-y-4">
            <div
              v-for="history in processHistory"
              :key="history.historyId"
              class="flex gap-3"
            >
              <div class="flex-shrink-0 w-2 h-2 mt-2 rounded-full bg-blue-500"></div>
              <div>
                <p class="text-sm font-medium">
                  {{ history.fromStepName || '시작' }} → {{ history.toStepName }}
                </p>
                <p v-if="history.reason" class="text-sm text-gray-500">{{ history.reason }}</p>
                <p class="text-xs text-gray-400">{{ formatDate(history.createdAt) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useApplyStore } from '@/stores/apply'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import { ProcessStepNames, ProcessStepColors, type ProcessStep, type ApplyDetailResponse } from '@/types/apply'

const applyStore = useApplyStore()
const authStore = useAuthStore()

const { myApplies, processHistory, userStats, loading, error } = storeToRefs(applyStore)

const activeFilter = ref('all')
const showHistoryModal = ref(false)
const historyLoading = ref(false)

const filters = [
  { label: '전체', value: 'all' },
  { label: '진행 중', value: 'pending' },
  { label: '합격', value: 'passed' },
  { label: '불합격', value: 'failed' },
]

const filteredApplies = computed(() => {
  switch (activeFilter.value) {
    case 'pending':
      return myApplies.value.filter(a => !a.completed)
    case 'passed':
      return myApplies.value.filter(a => a.passed)
    case 'failed':
      return myApplies.value.filter(a => a.failed)
    default:
      return myApplies.value
  }
})

function getStepColor(step: ProcessStep): string {
  return ProcessStepColors[step] || 'bg-gray-100 text-gray-800'
}

function getStepName(step: string): string {
  return ProcessStepNames[step as ProcessStep] || step
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function viewHistory(apply: ApplyDetailResponse) {
  showHistoryModal.value = true
  historyLoading.value = true
  try {
    await applyStore.fetchProcessHistory(apply.processId)
  } finally {
    historyLoading.value = false
  }
}

async function handleCancel(apply: ApplyDetailResponse) {
  if (!confirm('정말 지원을 취소하시겠습니까?')) return
  
  try {
    await applyStore.cancelApply(apply.applyId, authStore.user!.userId)
    alert('지원이 취소되었습니다.')
  } catch (err) {
    alert('지원 취소에 실패했습니다.')
  }
}

async function fetchData() {
  if (!authStore.user) return
  await Promise.all([
    applyStore.fetchMyApplies(authStore.user.userId),
    applyStore.fetchUserStats(authStore.user.userId),
  ])
}

onMounted(fetchData)
</script>
