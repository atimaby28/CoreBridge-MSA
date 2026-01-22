<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 헤더 -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900">채용공고</h1>
        <p class="mt-2 text-gray-600">다양한 기업의 채용 정보를 확인하세요</p>
      </div>

      <!-- 게시판 탭 -->
      <div class="mb-6">
        <div class="border-b border-gray-200">
          <nav class="-mb-px flex space-x-8" aria-label="Tabs">
            <button
              v-for="board in boards"
              :key="board.boardId"
              @click="selectBoard(board.boardId)"
              :class="[
                currentBoardId === board.boardId
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300',
                'whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors'
              ]"
            >
              {{ board.name }}
            </button>
          </nav>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="flex justify-center items-center py-20">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <p class="text-red-600">{{ error }}</p>
        <button @click="retry" class="mt-2 text-sm text-red-700 underline">다시 시도</button>
      </div>

      <!-- 채용공고 목록 -->
      <div v-else>
        <!-- 빈 목록 -->
        <div v-if="jobpostings.length === 0" class="text-center py-20">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-gray-900">채용공고가 없습니다</h3>
          <p class="mt-2 text-gray-500">아직 등록된 채용공고가 없습니다.</p>
        </div>

        <!-- 목록 -->
        <div v-else class="space-y-4">
          <div
            v-for="jobposting in jobpostings"
            :key="jobposting.jobpostingId"
            @click="goToDetail(jobposting.jobpostingId)"
            class="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow cursor-pointer"
          >
            <div class="flex justify-between items-start">
              <div class="flex-1">
                <h2 class="text-lg font-semibold text-gray-900 hover:text-indigo-600 transition-colors">
                  {{ jobposting.title }}
                </h2>
                <p class="mt-2 text-gray-600 line-clamp-2">
                  {{ truncateContent(jobposting.content) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm text-gray-500 space-x-4">
              <span>{{ formatDate(jobposting.createdAt) }}</span>
              <span>•</span>
              <span>작성자 ID: {{ jobposting.userId }}</span>
            </div>
          </div>
        </div>

        <!-- 페이지네이션 -->
        <div v-if="totalPages > 1" class="mt-8 flex justify-center">
          <nav class="flex items-center space-x-2">
            <button
              @click="goToPage(currentPage - 1)"
              :disabled="!hasPrevPage"
              :class="[
                hasPrevPage ? 'text-gray-700 hover:bg-gray-100' : 'text-gray-300 cursor-not-allowed',
                'px-3 py-2 rounded-md text-sm font-medium'
              ]"
            >
              이전
            </button>
            
            <template v-for="page in visiblePages" :key="page">
              <button
                v-if="page !== '...'"
                @click="goToPage(page as number)"
                :class="[
                  page === currentPage
                    ? 'bg-indigo-600 text-white'
                    : 'text-gray-700 hover:bg-gray-100',
                  'px-4 py-2 rounded-md text-sm font-medium'
                ]"
              >
                {{ page }}
              </button>
              <span v-else class="px-2 text-gray-400">...</span>
            </template>
            
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="!hasNextPage"
              :class="[
                hasNextPage ? 'text-gray-700 hover:bg-gray-100' : 'text-gray-300 cursor-not-allowed',
                'px-3 py-2 rounded-md text-sm font-medium'
              ]"
            >
              다음
            </button>
          </nav>
        </div>

        <!-- 총 개수 -->
        <div class="mt-4 text-center text-sm text-gray-500">
          총 {{ totalCount }}개의 채용공고
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useJobpostingStore } from '@/stores/jobposting'
import { DEFAULT_BOARDS } from '@/types/jobposting'

const router = useRouter()
const route = useRoute()
const jobpostingStore = useJobpostingStore()

const { 
  jobpostings, 
  loading, 
  error, 
  currentPage, 
  totalCount, 
  totalPages, 
  hasNextPage, 
  hasPrevPage,
  currentBoardId,
} = storeToRefs(jobpostingStore)

// 게시판 목록
const boards = ref(DEFAULT_BOARDS)

// 표시할 페이지 번호들
const visiblePages = computed(() => {
  const pages: (number | string)[] = []
  const total = totalPages.value
  const current = currentPage.value
  
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 3) {
      pages.push(1, 2, 3, 4, '...', total)
    } else if (current >= total - 2) {
      pages.push(1, '...', total - 3, total - 2, total - 1, total)
    } else {
      pages.push(1, '...', current - 1, current, current + 1, '...', total)
    }
  }
  
  return pages
})

// 게시판 선택
function selectBoard(boardId: number) {
  jobpostingStore.setBoardId(boardId)
  jobpostingStore.fetchJobpostings()
}

// 페이지 이동
function goToPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  jobpostingStore.setPage(page)
  jobpostingStore.fetchJobpostings()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 상세 페이지로 이동
function goToDetail(jobpostingId: number) {
  router.push(`/jobpostings/${jobpostingId}`)
}

// 재시도
function retry() {
  jobpostingStore.fetchJobpostings()
}

// 날짜 포맷
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// 내용 자르기
function truncateContent(content: string, maxLength: number = 150): string {
  if (content.length <= maxLength) return content
  return content.slice(0, maxLength) + '...'
}

// 초기 로드
onMounted(() => {
  // URL에서 boardId 파라미터 확인
  const boardId = route.query.boardId ? Number(route.query.boardId) : 1
  const page = route.query.page ? Number(route.query.page) : 1
  
  jobpostingStore.setBoardId(boardId)
  jobpostingStore.setPage(page)
  jobpostingStore.fetchJobpostings()
})

// URL 쿼리 변경 감지
watch(() => route.query, (newQuery) => {
  if (newQuery.boardId) {
    jobpostingStore.setBoardId(Number(newQuery.boardId))
  }
  if (newQuery.page) {
    jobpostingStore.setPage(Number(newQuery.page))
  }
  jobpostingStore.fetchJobpostings()
}, { deep: true })
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
