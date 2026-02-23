<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useScheduleStore } from '@/stores/schedule'
import type { ScheduleType, CreateScheduleRequest } from '@/types/schedule'
import { SCHEDULE_TYPE_LABELS } from '@/types/schedule'

// Props로 지원자 정보 받기
const props = defineProps<{
  applicant?: {
    applyId: number
    userId: number
    jobpostingId: number
    currentStep?: string
    jobpostingTitle?: string
  }
}>()

const emit = defineEmits<{
  close: []
  created: []
}>()

const scheduleStore = useScheduleStore()

// ============================================
// Form State
// ============================================
const form = ref<CreateScheduleRequest>({
  applyId: props.applicant?.applyId || 0,
  jobpostingId: props.applicant?.jobpostingId || 0,
  userId: props.applicant?.userId || 0,
  type: 'INTERVIEW_1',
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  interviewerId: undefined,
  interviewerName: ''
})

const loading = ref(false)
const error = ref<string | null>(null)
const conflictWarning = ref<string | null>(null)

// ============================================
// Computed
// ============================================
const scheduleTypes: ScheduleType[] = [
  'CODING_TEST',
  'INTERVIEW_1',
  'INTERVIEW_2',
  'FINAL_INTERVIEW',
  'ORIENTATION',
  'OTHER'
]

const isFormValid = computed(() => {
  return form.value.applyId > 0 &&
    form.value.jobpostingId > 0 &&
    form.value.userId > 0 &&
    form.value.title.trim() !== '' &&
    form.value.startTime !== '' &&
    form.value.endTime !== '' &&
    new Date(form.value.endTime) > new Date(form.value.startTime)
})

const hasApplicantInfo = computed(() => !!props.applicant)

// ============================================
// Methods
// ============================================

// 현재 단계에 맞는 일정 타입 추천
function getRecommendedType(currentStep?: string): ScheduleType {
  if (!currentStep) return 'INTERVIEW_1'
  
  const stepTypeMap: Record<string, ScheduleType> = {
    'DOCUMENT_PASS': 'CODING_TEST',
    'CODING_TEST_PASS': 'INTERVIEW_1',
    'INTERVIEW_1_PASS': 'INTERVIEW_2',
    'INTERVIEW_2_PASS': 'FINAL_INTERVIEW',
    'FINAL_PASS': 'ORIENTATION'
  }
  
  return stepTypeMap[currentStep] || 'INTERVIEW_1'
}

function setDefaultTitle() {
  const typeLabel = SCHEDULE_TYPE_LABELS[form.value.type]
  form.value.title = typeLabel
}

async function checkConflict() {
  if (!form.value.userId || !form.value.startTime || !form.value.endTime) {
    return
  }
  
  try {
    const result = await scheduleStore.checkConflict({
      userId: form.value.userId,
      interviewerId: form.value.interviewerId,
      startTime: form.value.startTime,
      endTime: form.value.endTime
    })
    
    if (result.hasConflict) {
      conflictWarning.value = result.conflicts
        .map(c => c.message)
        .join('\n')
    } else {
      conflictWarning.value = null
    }
  } catch (e) {
    console.error('충돌 체크 실패:', e)
  }
}

async function handleSubmit() {
  if (!isFormValid.value) return
  
  try {
    loading.value = true
    error.value = null
    
    await scheduleStore.createSchedule({
      ...form.value,
      interviewerId: form.value.interviewerId || undefined
    })
    
    emit('created')
  } catch (e: any) {
    error.value = e.message || '일정 생성에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

function onTimeChange() {
  checkConflict()
}

// 기본 시간 설정 (내일 10:00 ~ 11:00)
function setDefaultTime() {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(10, 0, 0, 0)
  
  const endTime = new Date(tomorrow)
  endTime.setHours(11, 0, 0, 0)
  
  form.value.startTime = tomorrow.toISOString().slice(0, 16)
  form.value.endTime = endTime.toISOString().slice(0, 16)
}

// 초기화
watch(() => props.applicant, (newVal) => {
  if (newVal) {
    form.value.applyId = newVal.applyId
    form.value.jobpostingId = newVal.jobpostingId
    form.value.userId = newVal.userId
    form.value.type = getRecommendedType(newVal.currentStep)
    setDefaultTitle()
    setDefaultTime()
  }
}, { immediate: true })
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center">
    <!-- 배경 오버레이 -->
    <div 
      class="absolute inset-0 bg-black/50" 
      @click="emit('close')"
    ></div>
    
    <!-- 모달 컨텐츠 -->
    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
      <!-- 헤더 -->
      <div class="p-6 border-b">
        <div class="flex items-center justify-between">
          <h2 class="text-xl font-bold text-gray-900">📅 일정 등록</h2>
          <button
            @click="emit('close')"
            class="p-2 hover:bg-gray-100 rounded-lg text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>
      </div>

      <!-- 폼 -->
      <form @submit.prevent="handleSubmit" class="p-6 space-y-4">
        <!-- 에러 메시지 -->
        <div v-if="error" class="bg-red-50 text-red-600 p-3 rounded-lg text-sm">
          {{ error }}
        </div>

        <!-- 충돌 경고 -->
        <div v-if="conflictWarning" class="bg-yellow-50 text-yellow-700 p-3 rounded-lg text-sm">
          ⚠️ {{ conflictWarning }}
        </div>

        <!-- 지원자 정보 (자동 입력된 경우) -->
        <div v-if="hasApplicantInfo" class="bg-blue-50 p-4 rounded-lg">
          <p class="text-sm font-medium text-blue-800 mb-1">지원자 정보</p>
          <p class="text-sm text-blue-600">
            지원번호: #{{ form.applyId }} · 
            지원자 ID: {{ form.userId }}
          </p>
          <p v-if="applicant?.jobpostingTitle" class="text-sm text-blue-600">
            공고: {{ applicant.jobpostingTitle }}
          </p>
        </div>

        <!-- 수동 입력 (applicant props 없을 때) -->
        <div v-else class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              지원 ID *
            </label>
            <input
              v-model.number="form.applyId"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="지원 ID"
              required
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              공고 ID *
            </label>
            <input
              v-model.number="form.jobpostingId"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="공고 ID"
              required
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              지원자 ID *
            </label>
            <input
              v-model.number="form.userId"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="지원자 ID"
              required
              @change="checkConflict"
            />
          </div>
        </div>

        <!-- 일정 유형 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            일정 유형 *
          </label>
          <select
            v-model="form.type"
            @change="setDefaultTitle"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          >
            <option v-for="type in scheduleTypes" :key="type" :value="type">
              {{ SCHEDULE_TYPE_LABELS[type] }}
            </option>
          </select>
        </div>

        <!-- 제목 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            제목 *
          </label>
          <input
            v-model="form.title"
            type="text"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            placeholder="일정 제목"
            required
          />
        </div>

        <!-- 시간 -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              시작 시간 *
            </label>
            <input
              v-model="form.startTime"
              type="datetime-local"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              required
              @change="onTimeChange"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              종료 시간 *
            </label>
            <input
              v-model="form.endTime"
              type="datetime-local"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              required
              @change="onTimeChange"
            />
          </div>
        </div>

        <!-- 장소 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            장소
          </label>
          <input
            v-model="form.location"
            type="text"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            placeholder="장소 또는 화상회의 링크"
          />
        </div>

        <!-- 면접관 -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              면접관 ID
            </label>
            <input
              v-model.number="form.interviewerId"
              type="number"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="면접관 ID"
              @change="checkConflict"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              면접관 이름
            </label>
            <input
              v-model="form.interviewerName"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="면접관 이름"
            />
          </div>
        </div>

        <!-- 설명 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            안내사항
          </label>
          <textarea
            v-model="form.description"
            rows="3"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            placeholder="지원자에게 전달할 안내사항"
          ></textarea>
        </div>

        <!-- 버튼 -->
        <div class="flex justify-end gap-3 pt-4">
          <button
            type="button"
            @click="emit('close')"
            class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            type="submit"
            :disabled="!isFormValid || loading"
            class="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {{ loading ? '등록 중...' : '일정 등록' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
