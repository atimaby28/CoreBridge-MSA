<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useJobpostingStore } from '@/stores/jobposting'
import { useAuthStore } from '@/stores/auth'
import { hotService } from '@/api/jobposting'
import { DEFAULT_BOARDS } from '@/types/jobposting'
import type { HotJobpostingResponse } from '@/types/jobposting'

const router = useRouter()
const route = useRoute()
const jobpostingStore = useJobpostingStore()
const authStore = useAuthStore()

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

// 기업 또는 관리자만 공고 등록 가능
const canCreateJobposting = computed(() => authStore.isCompany || authStore.isAdmin)

// 게시판 목록
const boards = ref(DEFAULT_BOARDS)

// 인기 공고
const hotJobpostings = ref<HotJobpostingResponse[]>([])
const hotLoading = ref(false)

// 검색어
const searchKeyword = ref('')

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

// 인기 공고 로드
async function loadHotJobpostings() {
  hotLoading.value = true
  try {
    hotJobpostings.value = await hotService.getHotToday()
  } catch (e) {
    console.error('인기 공고 로드 실패:', e)
    hotJobpostings.value = []
  } finally {
    hotLoading.value = false
  }
}

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
function truncateContent(content: string, maxLength: number = 120): string {
  if (content.length <= maxLength) return content
  return content.slice(0, maxLength) + '...'
}

// 초기 로드
onMounted(() => {
  const boardId = route.query.boardId ? Number(route.query.boardId) : 1
  const page = route.query.page ? Number(route.query.page) : 1
  
  jobpostingStore.setBoardId(boardId)
  jobpostingStore.setPage(page)
  jobpostingStore.fetchJobpostings()
  
  loadHotJobpostings()
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

<template>
  <div>
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">채용공고</h1>
        <p class="text-gray-500 mt-1">{{ totalCount }}개의 채용공고</p>
      </div>
      <router-link 
        v-if="canCreateJobposting" 
        to="/jobpostings/new" 
        class="btn btn-primary"
      >
        + 공고 등록
      </router-link>
    </div>

    <!-- 필터 카드 -->
    <div class="card mb-6">
      <div class="flex flex-col sm:flex-row items-start sm:items-center gap-4">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="검색어를 입력하세요"
          class="input max-w-xs"
        />
        <div class="flex items-center space-x-2">
          <button
            v-for="board in boards"
            :key="board.boardId"
            @click="selectBoard(board.boardId)"
            :class="[
              currentBoardId === board.boardId
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200',
              'px-4 py-2 rounded-lg text-sm font-medium transition-colors'
            ]"
          >
            {{ board.name }}
          </button>
        </div>
      </div>
    </div>

    <!-- 2컬럼 레이아웃 -->
    <div class="flex flex-col lg:flex-row gap-6">
      <!-- 왼쪽: 채용공고 목록 -->
      <div class="flex-1">
        <!-- 로딩 -->
        <div v-if="loading" class="card py-12 text-center text-gray-500">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto mb-2"></div>
          로딩중...
        </div>

        <!-- 에러 -->
        <div v-else-if="error" class="card py-12 text-center">
          <p class="text-red-600 mb-2">{{ error }}</p>
          <button @click="retry" class="text-sm text-primary-600 hover:underline">다시 시도</button>
        </div>

        <!-- 빈 목록 -->
        <div v-else-if="jobpostings.length === 0" class="card py-12 text-center text-gray-500">
          <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p>등록된 채용공고가 없습니다</p>
        </div>

        <!-- 목록 -->
        <div v-else class="space-y-4">
          <div
            v-for="jobposting in jobpostings"
            :key="jobposting.jobpostingId"
            @click="goToDetail(jobposting.jobpostingId)"
            class="card hover:shadow-md transition-shadow cursor-pointer"
          >
            <div class="flex items-start justify-between">
              <div class="flex-1 min-w-0">
                <h3 class="text-lg font-semibold text-gray-900 truncate hover:text-primary-600 transition-colors">
                  {{ jobposting.title }}
                </h3>
                <p class="text-gray-600 mt-1 line-clamp-2">
                  {{ truncateContent(jobposting.content) }}
                </p>
                <div class="flex items-center space-x-4 mt-3 text-sm text-gray-500">
                  <span>조회 {{ jobposting.viewCount || 0 }}</span>
                  <span>좋아요 {{ jobposting.likeCount || 0 }}</span>
                  <span>댓글 {{ jobposting.commentCount || 0 }}</span>
                  <span>{{ formatDate(jobposting.createdAt) }}</span>
                </div>
              </div>
              <div class="ml-4 flex flex-col items-end flex-shrink-0">
                <span class="badge badge-blue">채용중</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 페이지네이션 -->
        <div v-if="totalPages > 1" class="flex justify-center mt-8">
          <div class="flex items-center space-x-2">
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
                    ? 'bg-primary-600 text-white'
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
          </div>
        </div>
      </div>

      <!-- 오른쪽: 인기 공고 사이드바 -->
      <div class="lg:w-80 flex-shrink-0">
        <div class="card sticky top-4">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-gray-900">🔥 인기 공고</h2>
          </div>

          <!-- 로딩 -->
          <div v-if="hotLoading" class="py-8 text-center text-gray-500">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600 mx-auto"></div>
          </div>

          <!-- 빈 목록 -->
          <div v-else-if="hotJobpostings.length === 0" class="py-8 text-center text-gray-500 text-sm">
            아직 인기 공고가 없습니다
          </div>

          <!-- 인기 공고 목록 -->
          <div v-else class="space-y-1">
            <div
              v-for="(hot, index) in hotJobpostings.slice(0, 5)"
              :key="hot.jobpostingId"
              @click="goToDetail(hot.jobpostingId)"
              class="flex items-center space-x-3 p-3 hover:bg-gray-50 rounded-lg transition-colors cursor-pointer"
            >
              <span class="font-bold text-primary-600 w-5">{{ index + 1 }}</span>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-900 truncate">{{ hot.title }}</p>
                <p class="text-xs text-gray-500">
                  조회 {{ hot.viewCount }} · 좋아요 {{ hot.likeCount }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
