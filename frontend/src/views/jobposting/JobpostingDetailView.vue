<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 뒤로가기 -->
      <button
        @click="goBack"
        class="mb-6 flex items-center text-gray-600 hover:text-gray-900 transition-colors"
      >
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        목록으로
      </button>

      <!-- 로딩 -->
      <div v-if="loading" class="flex justify-center items-center py-20">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-lg p-6">
        <p class="text-red-600">{{ error }}</p>
        <button @click="retry" class="mt-2 text-sm text-red-700 underline">다시 시도</button>
      </div>

      <!-- 채용공고 상세 -->
      <div v-else-if="jobposting" class="space-y-6">
        <!-- 메인 카드 -->
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <!-- 헤더 -->
          <div class="px-6 py-5 border-b border-gray-200">
            <h1 class="text-2xl font-bold text-gray-900">{{ jobposting.title }}</h1>
            <div class="mt-3 flex flex-wrap items-center text-sm text-gray-500 gap-4">
              <span>작성자 ID: {{ jobposting.userId }}</span>
              <span>•</span>
              <span>{{ formatDate(jobposting.createdAt) }}</span>
              <span v-if="jobposting.updatedAt !== jobposting.createdAt">
                • 수정됨: {{ formatDate(jobposting.updatedAt) }}
              </span>
            </div>
          </div>

          <!-- 내용 -->
          <div class="px-6 py-6">
            <div class="prose max-w-none whitespace-pre-wrap">
              {{ jobposting.content }}
            </div>
          </div>

          <!-- 통계 & 액션 -->
          <div class="px-6 py-4 bg-gray-50 border-t border-gray-200">
            <div class="flex items-center justify-between">
              <!-- 통계 -->
              <div class="flex items-center space-x-6 text-sm text-gray-500">
                <div class="flex items-center">
                  <svg class="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  조회 {{ jobposting.viewCount }}
                </div>
                <div class="flex items-center">
                  <svg class="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                  댓글 {{ jobposting.commentCount }}
                </div>
              </div>

              <!-- 좋아요 버튼 -->
              <button
                v-if="isAuthenticated"
                @click="handleLike"
                :class="[
                  jobposting.isLiked
                    ? 'bg-red-100 text-red-600 border-red-200'
                    : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50',
                  'flex items-center px-4 py-2 rounded-lg border transition-colors'
                ]"
              >
                <svg
                  :class="[jobposting.isLiked ? 'text-red-500' : 'text-gray-400']"
                  class="w-5 h-5 mr-1.5"
                  :fill="jobposting.isLiked ? 'currentColor' : 'none'"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                좋아요 {{ jobposting.likeCount }}
              </button>
              <div v-else class="flex items-center text-gray-400">
                <svg class="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                좋아요 {{ jobposting.likeCount }}
              </div>
            </div>
          </div>

          <!-- 수정/삭제 버튼 (작성자만) -->
          <div
            v-if="isOwner"
            class="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3"
          >
            <button
              @click="goToEdit"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
            >
              수정
            </button>
            <button
              @click="confirmDelete"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 transition-colors"
            >
              삭제
            </button>
          </div>
        </div>

        <!-- 댓글 섹션 -->
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">
              댓글 {{ jobposting.commentCount }}개
            </h2>
          </div>

          <!-- 댓글 작성 -->
          <div v-if="isAuthenticated" class="px-6 py-4 border-b border-gray-200">
            <textarea
              v-model="newComment"
              rows="3"
              placeholder="댓글을 작성하세요..."
              class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
            ></textarea>
            <div class="mt-3 flex justify-end">
              <button
                @click="submitComment"
                :disabled="!newComment.trim() || submittingComment"
                :class="[
                  newComment.trim() && !submittingComment
                    ? 'bg-indigo-600 hover:bg-indigo-700 text-white'
                    : 'bg-gray-300 text-gray-500 cursor-not-allowed',
                  'px-4 py-2 rounded-md text-sm font-medium transition-colors'
                ]"
              >
                {{ submittingComment ? '등록 중...' : '댓글 등록' }}
              </button>
            </div>
          </div>
          <div v-else class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <p class="text-gray-500 text-center">
              <router-link to="/auth/login" class="text-indigo-600 hover:underline">로그인</router-link>
              하시면 댓글을 작성할 수 있습니다.
            </p>
          </div>

          <!-- 댓글 목록 -->
          <div class="divide-y divide-gray-200">
            <div v-if="comments.length === 0" class="px-6 py-8 text-center text-gray-500">
              아직 댓글이 없습니다. 첫 번째 댓글을 작성해보세요!
            </div>
            <div
              v-for="comment in comments"
              :key="comment.commentId"
              class="px-6 py-4"
            >
              <div class="flex justify-between items-start">
                <div class="flex-1">
                  <div class="flex items-center space-x-2">
                    <span class="font-medium text-gray-900">사용자 {{ comment.userId }}</span>
                    <span class="text-sm text-gray-500">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                  <p class="mt-2 text-gray-700 whitespace-pre-wrap">{{ comment.content }}</p>
                </div>
                <button
                  v-if="comment.userId === userId"
                  @click="handleDeleteComment(comment.commentId)"
                  class="text-gray-400 hover:text-red-500 transition-colors"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 댓글 페이지네이션 -->
          <div v-if="commentTotalPages > 1" class="px-6 py-4 border-t border-gray-200 flex justify-center">
            <nav class="flex items-center space-x-2">
              <button
                v-for="page in commentTotalPages"
                :key="page"
                @click="loadComments(page)"
                :class="[
                  page === commentPage
                    ? 'bg-indigo-600 text-white'
                    : 'text-gray-700 hover:bg-gray-100',
                  'px-3 py-1 rounded text-sm'
                ]"
              >
                {{ page }}
              </button>
            </nav>
          </div>
        </div>
      </div>

      <!-- 삭제 확인 모달 -->
      <div
        v-if="showDeleteModal"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
        @click.self="showDeleteModal = false"
      >
        <div class="bg-white rounded-lg p-6 max-w-md w-full mx-4">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">채용공고 삭제</h3>
          <p class="text-gray-600 mb-6">정말로 이 채용공고를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.</p>
          <div class="flex justify-end space-x-3">
            <button
              @click="showDeleteModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
            >
              취소
            </button>
            <button
              @click="handleDelete"
              :disabled="deleting"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 disabled:opacity-50"
            >
              {{ deleting ? '삭제 중...' : '삭제' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useJobpostingStore } from '@/stores/jobposting'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const jobpostingStore = useJobpostingStore()
const authStore = useAuthStore()

const { currentJobposting: jobposting, comments, loading, error, commentPage, commentTotalPages } = storeToRefs(jobpostingStore)
const { isAuthenticated, userId } = storeToRefs(authStore)

// 로컬 상태
const newComment = ref('')
const submittingComment = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)

// 작성자 여부
const isOwner = computed(() => {
  return isAuthenticated.value && jobposting.value?.userId === userId.value
})

// 뒤로가기
function goBack() {
  router.push('/jobpostings')
}

// 수정 페이지로
function goToEdit() {
  if (jobposting.value) {
    router.push(`/jobpostings/${jobposting.value.jobpostingId}/edit`)
  }
}

// 삭제 확인
function confirmDelete() {
  showDeleteModal.value = true
}

// 삭제 실행
async function handleDelete() {
  if (!jobposting.value) return
  
  deleting.value = true
  try {
    await jobpostingStore.deleteJobposting(jobposting.value.jobpostingId)
    router.push('/jobpostings')
  } catch (e) {
    alert('삭제에 실패했습니다.')
  } finally {
    deleting.value = false
    showDeleteModal.value = false
  }
}

// 좋아요 토글
async function handleLike() {
  if (!isAuthenticated.value || !jobposting.value || !userId.value) return
  
  try {
    await jobpostingStore.toggleLike(jobposting.value.jobpostingId, userId.value)
  } catch (e) {
    alert('좋아요 처리에 실패했습니다.')
  }
}

// 댓글 작성
async function submitComment() {
  if (!newComment.value.trim() || !jobposting.value) return
  
  submittingComment.value = true
  try {
    await jobpostingStore.createComment({
      jobpostingId: jobposting.value.jobpostingId,
      content: newComment.value.trim(),
    })
    newComment.value = ''
  } catch (e) {
    alert('댓글 작성에 실패했습니다.')
  } finally {
    submittingComment.value = false
  }
}

// 댓글 삭제
async function handleDeleteComment(commentId: number) {
  if (!confirm('댓글을 삭제하시겠습니까?')) return
  if (!jobposting.value) return
  
  try {
    await jobpostingStore.deleteComment(commentId, jobposting.value.jobpostingId)
  } catch (e) {
    alert('댓글 삭제에 실패했습니다.')
  }
}

// 댓글 페이지 로드
function loadComments(page: number) {
  if (!jobposting.value) return
  jobpostingStore.fetchComments(jobposting.value.jobpostingId, page)
}

// 재시도
function retry() {
  const jobpostingId = Number(route.params.id)
  jobpostingStore.fetchJobposting(jobpostingId, userId.value || undefined)
}

// 날짜 포맷
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 초기 로드
onMounted(async () => {
  const jobpostingId = Number(route.params.id)
  
  try {
    await jobpostingStore.fetchJobposting(jobpostingId, userId.value || undefined)
    await jobpostingStore.fetchComments(jobpostingId, 1)
  } catch (e) {
    console.error('Failed to load jobposting:', e)
  }
})

// 정리
onUnmounted(() => {
  jobpostingStore.clearCurrentJobposting()
})
</script>

<style scoped>
.prose {
  line-height: 1.75;
}
</style>
