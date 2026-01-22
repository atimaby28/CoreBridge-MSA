<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 뒤로가기 -->
      <button
        @click="goBack"
        class="mb-6 flex items-center text-gray-600 hover:text-gray-900 transition-colors"
      >
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        {{ isEditMode ? '상세로 돌아가기' : '목록으로' }}
      </button>

      <!-- 폼 카드 -->
      <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <!-- 헤더 -->
        <div class="px-6 py-5 border-b border-gray-200">
          <h1 class="text-2xl font-bold text-gray-900">
            {{ isEditMode ? '채용공고 수정' : '채용공고 등록' }}
          </h1>
          <p class="mt-1 text-sm text-gray-500">
            {{ isEditMode ? '채용공고 내용을 수정하세요' : '새로운 채용공고를 등록하세요' }}
          </p>
        </div>

        <!-- 로딩 (수정 모드에서 데이터 로드 중) -->
        <div v-if="isEditMode && loading" class="px-6 py-12 flex justify-center">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
        </div>

        <!-- 폼 -->
        <form v-else @submit.prevent="handleSubmit" class="px-6 py-6 space-y-6">
          <!-- 게시판 선택 (작성 시에만) -->
          <div v-if="!isEditMode">
            <label for="boardId" class="block text-sm font-medium text-gray-700 mb-2">
              게시판 <span class="text-red-500">*</span>
            </label>
            <select
              id="boardId"
              v-model="form.boardId"
              required
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            >
              <option value="" disabled>게시판을 선택하세요</option>
              <option v-for="board in boards" :key="board.boardId" :value="board.boardId">
                {{ board.name }}
              </option>
            </select>
          </div>

          <!-- 제목 -->
          <div>
            <label for="title" class="block text-sm font-medium text-gray-700 mb-2">
              제목 <span class="text-red-500">*</span>
            </label>
            <input
              id="title"
              v-model="form.title"
              type="text"
              required
              maxlength="100"
              placeholder="채용공고 제목을 입력하세요"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
            <p class="mt-1 text-sm text-gray-500">{{ form.title.length }}/100</p>
          </div>

          <!-- 내용 -->
          <div>
            <label for="content" class="block text-sm font-medium text-gray-700 mb-2">
              내용 <span class="text-red-500">*</span>
            </label>
            <textarea
              id="content"
              v-model="form.content"
              required
              rows="15"
              placeholder="채용공고 내용을 입력하세요&#10;&#10;예시:&#10;[담당업무]&#10;- 백엔드 API 개발&#10;- 데이터베이스 설계&#10;&#10;[자격요건]&#10;- Java/Spring 경험 3년 이상&#10;&#10;[우대사항]&#10;- MSA 경험&#10;- Kubernetes 경험"
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
            ></textarea>
          </div>

          <!-- 에러 메시지 -->
          <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4">
            <p class="text-red-600 text-sm">{{ error }}</p>
          </div>

          <!-- 버튼 -->
          <div class="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              @click="goBack"
              class="px-6 py-3 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              :disabled="submitting || !isFormValid"
              :class="[
                submitting || !isFormValid
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-indigo-600 hover:bg-indigo-700 text-white',
                'px-6 py-3 text-sm font-medium rounded-lg transition-colors'
              ]"
            >
              {{ submitting ? '처리 중...' : (isEditMode ? '수정하기' : '등록하기') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useJobpostingStore } from '@/stores/jobposting'
import { useAuthStore } from '@/stores/auth'
import { DEFAULT_BOARDS } from '@/types/jobposting'
import { jobpostingService } from '@/api/jobposting'

const router = useRouter()
const route = useRoute()
const jobpostingStore = useJobpostingStore()
const authStore = useAuthStore()

const { loading, error } = storeToRefs(jobpostingStore)
const { isAuthenticated, isCompany, userId } = storeToRefs(authStore)

// 수정 모드 여부
const isEditMode = computed(() => route.name === 'JobpostingEdit')
const jobpostingId = computed(() => isEditMode.value ? Number(route.params.id) : null)

// 게시판 목록
const boards = ref(DEFAULT_BOARDS.filter(b => b.boardId !== 1)) // '전체' 제외

// 폼 데이터
const form = ref({
  boardId: 2 as number | '',
  title: '',
  content: '',
})

// 제출 중 상태
const submitting = ref(false)

// 폼 유효성
const isFormValid = computed(() => {
  if (isEditMode.value) {
    return form.value.title.trim().length >= 2 && form.value.content.trim().length > 0
  }
  return (
    form.value.boardId !== '' &&
    form.value.title.trim().length >= 2 &&
    form.value.content.trim().length > 0
  )
})

// 뒤로가기
function goBack() {
  if (isEditMode.value && jobpostingId.value) {
    router.push(`/jobpostings/${jobpostingId.value}`)
  } else {
    router.push('/jobpostings')
  }
}

// 제출
async function handleSubmit() {
  if (!isFormValid.value || submitting.value) return
  
  submitting.value = true
  jobpostingStore.clearError()
  
  try {
    if (isEditMode.value && jobpostingId.value) {
      // 수정
      await jobpostingStore.updateJobposting(jobpostingId.value, {
        title: form.value.title.trim(),
        content: form.value.content.trim(),
      })
      router.push(`/jobpostings/${jobpostingId.value}`)
    } else {
      // 생성
      const jobposting = await jobpostingStore.createJobposting({
        boardId: form.value.boardId as number,
        title: form.value.title.trim(),
        content: form.value.content.trim(),
      })
      router.push(`/jobpostings/${jobposting.jobpostingId}`)
    }
  } catch (e) {
    console.error('Submit failed:', e)
  } finally {
    submitting.value = false
  }
}

// 수정 모드일 때 기존 데이터 로드
async function loadJobposting() {
  if (!isEditMode.value || !jobpostingId.value) return
  
  try {
    const jobposting = await jobpostingService.getById(jobpostingId.value)
    
    // 작성자 확인
    if (jobposting.userId !== userId.value) {
      alert('본인이 작성한 채용공고만 수정할 수 있습니다.')
      router.push(`/jobpostings/${jobpostingId.value}`)
      return
    }
    
    form.value.boardId = jobposting.boardId
    form.value.title = jobposting.title
    form.value.content = jobposting.content
  } catch (e) {
    console.error('Failed to load jobposting:', e)
    alert('채용공고를 불러오는데 실패했습니다.')
    router.push('/jobpostings')
  }
}

// 초기화
onMounted(() => {
  // 권한 체크
  if (!isAuthenticated.value) {
    router.push('/auth/login')
    return
  }
  
  // 수정 모드일 때 데이터 로드
  if (isEditMode.value) {
    loadJobposting()
  }
})

// 라우트 변경 감지
watch(() => route.params.id, () => {
  if (isEditMode.value) {
    loadJobposting()
  }
})
</script>
