<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 헤더 -->
      <div class="mb-8 flex justify-between items-center">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">내 채용공고</h1>
          <p class="mt-2 text-gray-600">등록한 채용공고를 관리하세요</p>
        </div>
        <router-link
          to="/company/jobpostings/new"
          class="inline-flex items-center px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          새 채용공고
        </router-link>
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
        <div v-if="myJobpostings.length === 0" class="text-center py-20 bg-white rounded-lg border border-gray-200">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-gray-900">등록된 채용공고가 없습니다</h3>
          <p class="mt-2 text-gray-500">새 채용공고를 등록해보세요.</p>
          <router-link
            to="/company/jobpostings/new"
            class="mt-6 inline-flex items-center px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
          >
            채용공고 등록하기
          </router-link>
        </div>

        <!-- 테이블 -->
        <div v-else class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  제목
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  게시판
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  등록일
                </th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  관리
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr
                v-for="jobposting in myJobpostings"
                :key="jobposting.jobpostingId"
                class="hover:bg-gray-50"
              >
                <td class="px-6 py-4">
                  <router-link
                    :to="`/jobpostings/${jobposting.jobpostingId}`"
                    class="text-indigo-600 hover:text-indigo-900 font-medium"
                  >
                    {{ jobposting.title }}
                  </router-link>
                </td>
                <td class="px-6 py-4 text-sm text-gray-500">
                  {{ getBoardName(jobposting.boardId) }}
                </td>
                <td class="px-6 py-4 text-sm text-gray-500">
                  {{ formatDate(jobposting.createdAt) }}
                </td>
                <td class="px-6 py-4 text-right text-sm space-x-2">
                  <router-link
                    :to="`/jobpostings/${jobposting.jobpostingId}/edit`"
                    class="text-indigo-600 hover:text-indigo-900"
                  >
                    수정
                  </router-link>
                  <button
                    @click="confirmDelete(jobposting)"
                    class="text-red-600 hover:text-red-900"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 총 개수 -->
        <div class="mt-4 text-sm text-gray-500">
          총 {{ myJobpostings.length }}개의 채용공고
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
          <p class="text-gray-600 mb-2">
            다음 채용공고를 삭제하시겠습니까?
          </p>
          <p class="font-medium text-gray-900 mb-6">
            "{{ deleteTarget?.title }}"
          </p>
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
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useJobpostingStore } from '@/stores/jobposting'
import { DEFAULT_BOARDS, type Jobposting } from '@/types/jobposting'

const jobpostingStore = useJobpostingStore()
const { myJobpostings, loading, error } = storeToRefs(jobpostingStore)

// 삭제 관련 상태
const showDeleteModal = ref(false)
const deleteTarget = ref<Jobposting | null>(null)
const deleting = ref(false)

// 게시판 이름 가져오기
function getBoardName(boardId: number): string {
  const board = DEFAULT_BOARDS.find(b => b.boardId === boardId)
  return board?.name || '알 수 없음'
}

// 날짜 포맷
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

// 삭제 확인
function confirmDelete(jobposting: Jobposting) {
  deleteTarget.value = jobposting
  showDeleteModal.value = true
}

// 삭제 실행
async function handleDelete() {
  if (!deleteTarget.value) return
  
  deleting.value = true
  try {
    await jobpostingStore.deleteJobposting(deleteTarget.value.jobpostingId)
    showDeleteModal.value = false
    deleteTarget.value = null
  } catch (e) {
    alert('삭제에 실패했습니다.')
  } finally {
    deleting.value = false
  }
}

// 재시도
function retry() {
  jobpostingStore.fetchMyJobpostings()
}

// 초기 로드
onMounted(() => {
  jobpostingStore.fetchMyJobpostings()
})
</script>
