<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <!-- ========================================== -->
    <!-- 1. 공고 목록 뷰 (selectedJobpostingId가 null일 때) -->
    <!-- ========================================== -->
    <template v-if="!selectedJobpostingId">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900">지원자 관리</h1>
        <p class="mt-2 text-gray-600">채용공고를 선택하여 지원자를 관리하세요.</p>
      </div>

      <!-- 공고 테이블 -->
      <div class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">공고 제목</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">상태</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">마감일</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="myJobpostings.length === 0">
              <td colspan="4" class="px-6 py-12 text-center text-gray-500">
                등록된 채용공고가 없습니다.
              </td>
            </tr>
            <tr 
              v-for="jp in myJobpostings" 
              :key="jp.jobpostingId" 
              class="hover:bg-gray-50 cursor-pointer"
              @click="selectJobposting(jp.jobpostingId)"
            >
              <td class="px-6 py-4">
                <p class="text-sm font-medium text-gray-900">{{ jp.title }}</p>
              </td>
              <td class="px-6 py-4">
                <span :class="[
                  'px-2 py-1 rounded-full text-xs font-medium',
                  isJobpostingOpen(jp) ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]">
                  {{ isJobpostingOpen(jp) ? '모집중' : '마감' }}
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">
                {{ formatDate(jp.deadline) }}
              </td>
              <td class="px-6 py-4">
                <button 
                  class="text-blue-600 hover:text-blue-800 text-sm font-medium"
                  @click.stop="selectJobposting(jp.jobpostingId)"
                >
                  지원자 보기 →
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- ========================================== -->
    <!-- 2. 지원자 관리 뷰 (selectedJobpostingId가 있을 때) -->
    <!-- ========================================== -->
    <template v-else>
      <!-- 뒤로가기 & 헤더 -->
      <div class="mb-6">
        <button 
          @click="selectedJobpostingId = null"
          class="flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <span class="mr-2">←</span>
          <span>공고 목록으로</span>
        </button>
        <h1 class="text-2xl font-bold text-gray-900">{{ currentJobposting?.title }}</h1>
        <p class="mt-1 text-gray-600">지원자 상태를 관리하세요.</p>
      </div>

      <!-- 통계 카드 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="bg-white rounded-lg shadow p-4">
          <p class="text-sm text-gray-500">총 지원자</p>
          <p class="text-2xl font-bold text-gray-900">{{ stats.total }}명</p>
        </div>
        <div class="bg-white rounded-lg shadow p-4">
          <p class="text-sm text-gray-500">진행중</p>
          <p class="text-2xl font-bold text-blue-600">{{ stats.pending }}명</p>
        </div>
        <div class="bg-white rounded-lg shadow p-4">
          <p class="text-sm text-gray-500">합격</p>
          <p class="text-2xl font-bold text-green-600">{{ stats.passed }}명</p>
        </div>
        <div class="bg-white rounded-lg shadow p-4">
          <p class="text-sm text-gray-500">불합격</p>
          <p class="text-2xl font-bold text-red-600">{{ stats.failed }}명</p>
        </div>
      </div>

      <!-- 뷰 모드 토글 -->
      <div class="flex justify-end mb-4">
        <div class="inline-flex rounded-lg border border-gray-200 bg-white p-1">
          <button
            @click="viewMode = 'list'"
            :class="[
              'px-4 py-2 text-sm font-medium rounded-md transition-colors',
              viewMode === 'list' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'
            ]"
          >
            리스트
          </button>
          <button
            @click="viewMode = 'kanban'"
            :class="[
              'px-4 py-2 text-sm font-medium rounded-md transition-colors',
              viewMode === 'kanban' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'
            ]"
          >
            칸반
          </button>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
        <p class="mt-4 text-gray-500">불러오는 중...</p>
      </div>

      <!-- 리스트 뷰 -->
      <div v-else-if="viewMode === 'list'" class="bg-white rounded-lg shadow">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase w-48">지원자</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase w-32">지원일</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase w-28">현재 단계</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase w-48">관리</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="jobpostingApplies.length === 0">
              <td colspan="4" class="px-6 py-12 text-center text-gray-500">
                지원자가 없습니다.
              </td>
            </tr>
            <tr v-for="applicant in jobpostingApplies" :key="applicant.applyId" class="hover:bg-gray-50">
              <td class="px-6 py-4">
                <div class="flex items-center">
                  <div class="flex-shrink-0 h-10 w-10 bg-blue-100 rounded-full flex items-center justify-center">
                    <span class="text-blue-600 font-medium text-xs">{{ getShortId(applicant.userId) }}</span>
                  </div>
                  <div class="ml-3">
                    <p class="text-sm font-medium text-gray-900">지원자 #{{ applicant.applyId }}</p>
                    <p class="text-xs text-gray-400">ID: {{ getShortId(applicant.userId) }}</p>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">
                {{ formatDate(applicant.appliedAt) }}
              </td>
              <td class="px-6 py-4">
                <span :class="['px-2 py-1 rounded-full text-xs font-medium whitespace-nowrap', getStepColor(applicant.currentStep)]">
                  {{ applicant.currentStepName }}
                </span>
              </td>
              <td class="px-6 py-4">
                <!-- 완료 상태 -->
                <span v-if="applicant.completed" class="text-sm text-gray-400">
                  {{ applicant.passed ? '✅ 최종합격' : '❌ 불합격' }}
                </span>
                <!-- 드롭다운 -->
                <div v-else class="flex items-center gap-2">
                  <!-- 일정 잡기 버튼 -->
                  <button
                    @click.stop="openScheduleModal(applicant)"
                    class="px-3 py-1.5 bg-green-50 text-green-700 rounded-lg text-sm font-medium hover:bg-green-100"
                    title="일정 잡기"
                  >
                    📅
                  </button>
                  <!-- 상태 변경 드롭다운 -->
                  <div class="dropdown-container">
                    <button
                      @click.stop="toggleDropdown(applicant.applyId, $event)"
                      class="px-3 py-1.5 bg-blue-50 text-blue-700 rounded-lg text-sm font-medium hover:bg-blue-100 flex items-center gap-1"
                    >
                      다음 단계
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                      </svg>
                    </button>
                    <!-- 드롭다운 메뉴 (Portal처럼 body에 고정) -->
                    <Teleport to="body">
                      <div
                        v-if="openDropdownId === applicant.applyId"
                        class="fixed bg-white rounded-lg shadow-xl border border-gray-200 py-1 z-[9999]"
                        :style="dropdownStyle"
                      >
                        <button
                          v-for="nextStep in applicant.allowedNextSteps"
                          :key="nextStep"
                          @click="handleTransition(applicant, nextStep)"
                          :class="[
                            'w-full px-4 py-2 text-left text-sm hover:bg-gray-100',
                            isPassStep(nextStep) ? 'text-green-700' :
                            isFailStep(nextStep) ? 'text-red-700' : 'text-gray-700'
                          ]"
                        >
                          {{ getStepName(nextStep) }}
                        </button>
                      </div>
                    </Teleport>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 칸반 뷰 -->
      <div v-else class="flex gap-4 overflow-x-auto pb-4">
        <div
          v-for="column in kanbanColumns"
          :key="column.step"
          class="flex-shrink-0 w-72 bg-gray-100 rounded-lg"
        >
          <!-- 컬럼 헤더 -->
          <div class="p-3 border-b border-gray-200">
            <div class="flex items-center justify-between">
              <span class="font-medium text-gray-900">{{ column.name }}</span>
              <span class="bg-gray-200 text-gray-700 px-2 py-0.5 rounded-full text-xs">
                {{ getApplicantsByStep(column.step).length }}
              </span>
            </div>
          </div>
          <!-- 카드 목록 -->
          <div
            class="p-2 min-h-[200px] space-y-2"
            @dragover.prevent
            @drop="handleDrop($event, column.step)"
          >
            <div
              v-for="applicant in getApplicantsByStep(column.step)"
              :key="applicant.applyId"
              :draggable="!applicant.completed"
              @dragstart="handleDragStart($event, applicant)"
              :class="[
                'bg-white rounded-lg shadow p-3',
                applicant.completed ? 'opacity-60' : 'cursor-move hover:shadow-md'
              ]"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="font-medium text-sm">지원자 #{{ applicant.userId }}</span>
                <span :class="['px-1.5 py-0.5 rounded text-xs', getStepColor(applicant.currentStep)]">
                  {{ applicant.currentStepName }}
                </span>
              </div>
              <p class="text-xs text-gray-500">{{ formatDate(applicant.appliedAt) }}</p>
              
              <!-- 칸반 카드 드롭다운 -->
              <div v-if="!applicant.completed" class="mt-2 flex gap-1">
                <button
                  @click.stop="openScheduleModal(applicant)"
                  class="flex-1 px-2 py-1 bg-green-50 text-green-600 rounded text-xs hover:bg-green-100"
                  title="일정 잡기"
                >
                  📅 일정
                </button>
                <button
                  @click.stop="toggleDropdown(applicant.applyId, $event)"
                  class="flex-1 px-2 py-1 bg-gray-50 text-gray-600 rounded text-xs hover:bg-gray-100 flex items-center justify-center gap-1"
                >
                  상태
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                  </svg>
                </button>
                <Teleport to="body">
                  <div
                    v-if="openDropdownId === applicant.applyId"
                    class="fixed bg-white rounded-lg shadow-xl border border-gray-200 py-1 z-[9999]"
                    :style="dropdownStyle"
                  >
                    <button
                      v-for="nextStep in applicant.allowedNextSteps"
                      :key="nextStep"
                      @click="handleTransition(applicant, nextStep)"
                      :class="[
                        'w-full px-3 py-1.5 text-left text-xs hover:bg-gray-100',
                        isPassStep(nextStep) ? 'text-green-700' :
                        isFailStep(nextStep) ? 'text-red-700' : 'text-gray-700'
                      ]"
                    >
                      {{ getStepName(nextStep) }}
                    </button>
                  </div>
                </Teleport>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 전이 확인 모달 -->
    <div v-if="showTransitionModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
        <h3 class="text-lg font-semibold mb-4">상태 변경 확인</h3>
        <p class="text-gray-600 mb-4">
          지원자 #{{ transitionTarget?.userId }}의 상태를 
          <span class="font-medium">{{ transitionTarget?.currentStepName }}</span>에서 
          <span class="font-medium">{{ getStepName(transitionNextStep!) }}</span>으로 
          변경하시겠습니까?
        </p>
        
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">사유 (선택)</label>
          <textarea
            v-model="transitionReason"
            rows="3"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
            placeholder="상태 변경 사유를 입력하세요"
          ></textarea>
        </div>

        <div class="flex justify-end gap-3">
          <button
            @click="closeModal"
            class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
          >
            취소
          </button>
          <button
            @click="confirmTransition"
            :disabled="transitioning"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {{ transitioning ? '처리 중...' : '확인' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 일정 생성 모달 -->
    <ScheduleCreateModal
      v-if="showScheduleModal && scheduleTarget"
      :applicant="scheduleTarget"
      @close="showScheduleModal = false"
      @created="handleScheduleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useApplyStore } from '@/stores/apply'
import { useJobpostingStore } from '@/stores/jobposting'
import { useAuthStore } from '@/stores/auth'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ProcessStepNames, ProcessStepColors, type ProcessStep, type ApplyDetailResponse } from '@/types/apply'
import ScheduleCreateModal from '@/components/schedule/ScheduleCreateModal.vue'

const route = useRoute()
const applyStore = useApplyStore()
const jobpostingStore = useJobpostingStore()
const authStore = useAuthStore()

const { jobpostingApplies, loading } = storeToRefs(applyStore)
const { myJobpostings } = storeToRefs(jobpostingStore)

// State
const selectedJobpostingId = ref<number | null>(null)
const viewMode = ref<'list' | 'kanban'>('list')
const openDropdownId = ref<number | null>(null)
const dropdownStyle = ref<{ top: string; left: string; minWidth: string }>({ top: '0px', left: '0px', minWidth: '180px' })
const showTransitionModal = ref(false)
const transitionTarget = ref<ApplyDetailResponse | null>(null)
const transitionNextStep = ref<string | null>(null)
const transitionReason = ref('')
const transitioning = ref(false)

// 일정 생성 모달 상태
const showScheduleModal = ref(false)
const scheduleTarget = ref<{
  applyId: number
  userId: number
  jobpostingId: number
  currentStep?: string
  jobpostingTitle?: string
} | null>(null)

// 현재 선택된 공고 정보
const currentJobposting = computed(() => {
  return myJobpostings.value.find(jp => jp.jobpostingId === selectedJobpostingId.value)
})

// 통계 계산 (실제 데이터 기반)
const stats = computed(() => {
  const applies = jobpostingApplies.value
  const total = applies.length
  const passed = applies.filter(a => a.currentStep === 'FINAL_PASS' || a.passed).length
  const failed = applies.filter(a => 
    a.currentStep?.includes('FAIL') || a.failed
  ).length
  const pending = total - passed - failed

  return { total, pending, passed, failed }
})

// 칸반 컬럼 정의
const kanbanColumns = [
  { step: 'APPLIED', name: '지원완료' },
  { step: 'DOCUMENT_REVIEW', name: '서류검토' },
  { step: 'DOCUMENT_PASS', name: '서류합격' },
  { step: 'CODING_TEST', name: '코딩테스트' },
  { step: 'INTERVIEW_1', name: '1차면접' },
  { step: 'INTERVIEW_2', name: '2차면접' },
  { step: 'FINAL_REVIEW', name: '최종검토' },
  { step: 'FINAL_PASS', name: '최종합격' },
]

// 단계별 지원자 필터
function getApplicantsByStep(step: string): ApplyDetailResponse[] {
  return jobpostingApplies.value.filter(a => {
    return a.currentStep === step || 
           a.currentStep === `${step}_PASS` ||
           a.currentStep === `${step}_FAIL`
  })
}

// 공고 선택
async function selectJobposting(jobpostingId: number) {
  selectedJobpostingId.value = jobpostingId
  await fetchApplicants()
}

// 지원자 목록 조회
async function fetchApplicants() {
  if (selectedJobpostingId.value) {
    await applyStore.fetchAppliesByJobposting(selectedJobpostingId.value)
  }
}

// 공고 모집 중 여부
function isJobpostingOpen(jp: any): boolean {
  if (!jp.deadline) return true
  return new Date(jp.deadline) > new Date()
}

// 날짜 포맷
function formatDate(dateString: string): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

// 단계 색상
function getStepColor(step: ProcessStep): string {
  return ProcessStepColors[step] || 'bg-gray-100 text-gray-800'
}

// 단계명
function getStepName(step: string): string {
  return ProcessStepNames[step as ProcessStep] || step
}

// 합격/불합격 단계 체크
function isPassStep(step: string): boolean {
  return step.includes('PASS')
}

function isFailStep(step: string): boolean {
  return step.includes('FAIL')
}

// ID 짧게 표시
function getShortId(id: number | string): string {
  const str = String(id)
  if (str.length <= 6) return str
  return str.slice(0, 4) + '...'
}

// 드롭다운 토글 (위치 계산 포함)
function toggleDropdown(applyId: number, event?: MouseEvent) {
  if (openDropdownId.value === applyId) {
    openDropdownId.value = null
    return
  }
  
  openDropdownId.value = applyId
  
  // 버튼 위치 계산
  if (event) {
    const button = event.currentTarget as HTMLElement
    const rect = button.getBoundingClientRect()
    dropdownStyle.value = {
      top: `${rect.bottom + 4}px`,
      left: `${rect.left}px`,
      minWidth: '180px'
    }
  }
}

// 드롭다운 외부 클릭 시 닫기
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  // 드롭다운 메뉴나 버튼 클릭이 아니면 닫기
  if (!target.closest('.dropdown-container') && !target.closest('[class*="z-[9999]"]')) {
    openDropdownId.value = null
  }
}

// 상태 전이 핸들러
function handleTransition(applicant: ApplyDetailResponse, nextStep: string) {
  transitionTarget.value = applicant
  transitionNextStep.value = nextStep
  transitionReason.value = ''
  openDropdownId.value = null
  showTransitionModal.value = true
}

// 일정 모달 열기
function openScheduleModal(applicant: ApplyDetailResponse) {
  scheduleTarget.value = {
    applyId: applicant.applyId,
    userId: applicant.userId,
    jobpostingId: selectedJobpostingId.value!,
    currentStep: applicant.currentStep,
    jobpostingTitle: currentJobposting.value?.title
  }
  showScheduleModal.value = true
}

// 일정 생성 완료
function handleScheduleCreated() {
  showScheduleModal.value = false
  scheduleTarget.value = null
  alert('일정이 등록되었습니다. 지원자에게 알림이 발송됩니다.')
}

// 모달 닫기
function closeModal() {
  showTransitionModal.value = false
  transitionTarget.value = null
  transitionNextStep.value = null
}

// 전이 확인
async function confirmTransition() {
  if (!transitionTarget.value || !transitionNextStep.value) return
  
  transitioning.value = true
  try {
    console.log('Transitioning:', {
      applyId: transitionTarget.value.applyId,
      nextStep: transitionNextStep.value,
      changedBy: authStore.user?.userId
    })
    
    await applyStore.transitionByApplyId(transitionTarget.value.applyId, {
      nextStep: transitionNextStep.value as ProcessStep,
      changedBy: authStore.user?.userId,
      reason: transitionReason.value || undefined,
    })
    
    closeModal()
    await fetchApplicants()
  } catch (err: any) {
    console.error('Transition error:', err)
    const message = err.response?.data?.message || err.message || '상태 변경에 실패했습니다.'
    alert(`상태 변경 실패: ${message}`)
  } finally {
    transitioning.value = false
  }
}

// 드래그 앤 드롭
let draggedApplicant: ApplyDetailResponse | null = null

function handleDragStart(event: DragEvent, applicant: ApplyDetailResponse) {
  draggedApplicant = applicant
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function handleDrop(event: DragEvent, targetStep: string) {
  event.preventDefault()
  if (!draggedApplicant || draggedApplicant.completed) return
  
  // 허용된 전이인지 확인
  if (draggedApplicant.allowedNextSteps.includes(targetStep)) {
    handleTransition(draggedApplicant, targetStep)
  } else {
    alert(`${draggedApplicant.currentStepName}에서 ${getStepName(targetStep)}으로 이동할 수 없습니다.`)
  }
  draggedApplicant = null
}

// 초기 데이터 로드
async function fetchData() {
  await jobpostingStore.fetchMyJobpostings()
  
  // URL 파라미터로 공고 ID가 있으면 선택
  const jobpostingId = route.query.jobpostingId
  if (jobpostingId) {
    selectedJobpostingId.value = Number(jobpostingId)
    await fetchApplicants()
  }
}

onMounted(() => {
  fetchData()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
