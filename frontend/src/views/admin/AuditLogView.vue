<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">감사 로그</h1>
      <p class="text-gray-600 mt-1">시스템 전체 API 호출 기록을 조회합니다</p>
    </div>

    <!-- 통계 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
      <div class="bg-white rounded-lg shadow p-4">
        <div class="text-sm text-gray-500">총 요청</div>
        <div class="text-2xl font-bold text-blue-600">{{ stats?.totalRequests?.toLocaleString() ?? '-' }}</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="text-sm text-gray-500">에러 수</div>
        <div class="text-2xl font-bold text-red-600">{{ stats?.errorCount?.toLocaleString() ?? '-' }}</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="text-sm text-gray-500">에러율</div>
        <div class="text-2xl font-bold" :class="errorRateClass">
          {{ errorRate }}%
        </div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="text-sm text-gray-500">고유 사용자</div>
        <div class="text-2xl font-bold text-gray-700">{{ stats?.uniqueUsers?.toLocaleString() ?? '-' }}</div>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <div class="text-sm text-gray-500">평균 응답시간</div>
        <div class="text-2xl font-bold text-gray-700">{{ avgExecutionTime }}ms</div>
      </div>
    </div>

    <!-- 필터 & 검색 -->
    <div class="bg-white rounded-lg shadow p-4 mb-6">
      <div class="flex flex-wrap gap-4 items-end">
        <!-- 키워드 검색 -->
        <div class="flex-[2] min-w-[200px]">
          <label class="block text-sm font-medium text-gray-700 mb-1">검색</label>
          <input 
            v-model="filters.keyword"
            type="text"
            placeholder="URI, 이메일, IP 검색..."
            class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-blue-500 focus:border-blue-500"
            @keyup.enter="applySearch"
          />
        </div>

        <!-- 서비스 필터 -->
        <div class="flex-1 min-w-[150px]">
          <label class="block text-sm font-medium text-gray-700 mb-1">서비스</label>
          <select v-model="filters.serviceName"
                  class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-blue-500 focus:border-blue-500">
            <option value="">전체</option>
            <option v-for="service in serviceNames" :key="service" :value="service">
              {{ service }}
            </option>
          </select>
        </div>

        <!-- 이벤트 타입 필터 -->
        <div class="flex-1 min-w-[150px]">
          <label class="block text-sm font-medium text-gray-700 mb-1">이벤트</label>
          <select v-model="filters.eventType"
                  class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-blue-500 focus:border-blue-500">
            <option value="">전체</option>
            <option v-for="(label, type) in eventTypeLabels" :key="type" :value="type">
              {{ label }}
            </option>
          </select>
        </div>

        <!-- 상태 필터 -->
        <div class="flex-1 min-w-[130px]">
          <label class="block text-sm font-medium text-gray-700 mb-1">상태</label>
          <select v-model="filters.statusFilter"
                  class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-blue-500 focus:border-blue-500">
            <option value="all">전체</option>
            <option value="success">성공 (2xx)</option>
            <option value="error">에러 (4xx, 5xx)</option>
          </select>
        </div>

        <!-- 검색 버튼 -->
        <button @click="applySearch"
                class="px-5 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          검색
        </button>

        <!-- 초기화 버튼 -->
        <button @click="resetFilters"
                class="px-4 py-2 bg-gray-100 text-gray-600 rounded-md hover:bg-gray-200 transition">
          초기화
        </button>
      </div>
    </div>

    <!-- 로그 테이블 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">시간</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">서비스</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">이벤트</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">메서드</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">URI</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">상태</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">실행시간</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">사용자</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">액션</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading" class="text-center">
              <td colspan="9" class="px-4 py-8 text-gray-500">로딩 중...</td>
            </tr>
            <tr v-else-if="displayedAudits.length === 0" class="text-center">
              <td colspan="9" class="px-4 py-8 text-gray-500">검색 결과가 없습니다</td>
            </tr>
            <tr v-for="audit in paginatedAudits" :key="audit.auditId" class="hover:bg-gray-50">
              <td class="px-4 py-3 text-sm text-gray-500 whitespace-nowrap">
                {{ formatDateTime(audit.createdAt) }}
              </td>
              <td class="px-4 py-3 text-sm">
                <span class="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
                  {{ audit.serviceName }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-700">{{ audit.eventTypeName }}</td>
              <td class="px-4 py-3 text-sm">
                <span :class="getMethodClass(audit.httpMethod)" class="px-2 py-1 text-xs font-medium rounded">
                  {{ audit.httpMethod }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 max-w-xs truncate" :title="audit.requestUri">
                {{ audit.requestUri }}
              </td>
              <td class="px-4 py-3 text-sm">
                <span :class="getStatusClass(audit.httpStatus)" class="px-2 py-1 text-xs font-medium rounded-full">
                  {{ audit.httpStatus }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ audit.executionTime }}ms</td>
              <td class="px-4 py-3 text-sm text-gray-500">
                {{ audit.userEmail || '-' }}
              </td>
              <td class="px-4 py-3 text-sm">
                <button @click="showDetail(audit)"
                        class="text-blue-600 hover:text-blue-800">
                  상세
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이지네이션 -->
      <div class="bg-gray-50 px-4 py-3 flex items-center justify-between border-t border-gray-200">
        <div class="text-sm text-gray-700">
          총 <span class="font-medium">{{ displayedAudits.length }}</span>개 중
          <span class="font-medium">{{ paginationStart }}</span> -
          <span class="font-medium">{{ paginationEnd }}</span>
        </div>
        <div class="flex gap-2">
          <button @click="prevPage" :disabled="currentPage === 0"
                  class="px-3 py-1 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100">
            이전
          </button>
          <span class="px-3 py-1 text-gray-600">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button @click="nextPage" :disabled="currentPage >= totalPages - 1"
                  class="px-3 py-1 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100">
            다음
          </button>
        </div>
      </div>
    </div>

    <!-- 상세 모달 -->
    <div v-if="selectedAudit" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
         @click.self="selectedAudit = null">
      <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
        <div class="p-6">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-bold">로그 상세</h3>
            <button @click="selectedAudit = null" class="text-gray-400 hover:text-gray-600 text-2xl">
              &times;
            </button>
          </div>

          <div class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="text-sm text-gray-500">ID</label>
                <div class="font-mono text-sm">{{ selectedAudit.auditId }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">생성 시간</label>
                <div>{{ formatDateTime(selectedAudit.createdAt) }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">서비스</label>
                <div>{{ selectedAudit.serviceName }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">이벤트 타입</label>
                <div>{{ selectedAudit.eventTypeName }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">HTTP 메서드</label>
                <div>{{ selectedAudit.httpMethod }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">HTTP 상태</label>
                <div>
                  <span :class="getStatusClass(selectedAudit.httpStatus)" class="px-2 py-1 text-xs font-medium rounded-full">
                    {{ selectedAudit.httpStatus }}
                  </span>
                </div>
              </div>
              <div>
                <label class="text-sm text-gray-500">실행 시간</label>
                <div>{{ selectedAudit.executionTime }}ms</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">사용자 ID</label>
                <div>{{ selectedAudit.userId ?? '-' }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">사용자 이메일</label>
                <div>{{ selectedAudit.userEmail ?? '-' }}</div>
              </div>
              <div>
                <label class="text-sm text-gray-500">클라이언트 IP</label>
                <div>{{ selectedAudit.clientIp ?? '-' }}</div>
              </div>
            </div>

            <div>
              <label class="text-sm text-gray-500">Request URI</label>
              <div class="font-mono text-sm bg-gray-100 p-2 rounded break-all">{{ selectedAudit.requestUri }}</div>
            </div>

            <div v-if="selectedAudit.requestBody">
              <label class="text-sm text-gray-500">Request Body</label>
              <pre class="text-xs bg-gray-100 p-2 rounded overflow-x-auto max-h-40">{{ formatJson(selectedAudit.requestBody) }}</pre>
            </div>

            <div v-if="selectedAudit.errorMessage">
              <label class="text-sm text-gray-500">에러 메시지</label>
              <div class="text-red-600 bg-red-50 p-2 rounded">{{ selectedAudit.errorMessage }}</div>
            </div>

            <div v-if="selectedAudit.userAgent">
              <label class="text-sm text-gray-500">User Agent</label>
              <div class="text-xs text-gray-500 break-all">{{ selectedAudit.userAgent }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getAuditsPaged,
  getAuditStats,
  EVENT_TYPE_LABELS,
  type AuditResponse,
  type AuditStatsResponse,
  type AuditEventType,
} from '@/api/audit'

// 상태
const audits = ref<AuditResponse[]>([])
const displayedAudits = ref<AuditResponse[]>([])
const stats = ref<AuditStatsResponse | null>(null)
const selectedAudit = ref<AuditResponse | null>(null)
const loading = ref(false)

// 페이지네이션
const currentPage = ref(0)
const pageSize = 20

// 필터 (검색 전 입력값)
const filters = ref({
  keyword: '',
  serviceName: '',
  eventType: '' as AuditEventType | '',
  statusFilter: 'all' as 'all' | 'success' | 'error',
})

// 이벤트 타입 라벨
const eventTypeLabels = EVENT_TYPE_LABELS

// 고유 서비스 목록
const serviceNames = computed(() => {
  const names = new Set(audits.value.map(a => a.serviceName))
  return Array.from(names).sort()
})

// 페이지네이션된 결과
const paginatedAudits = computed(() => {
  const start = currentPage.value * pageSize
  return displayedAudits.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.ceil(displayedAudits.value.length / pageSize) || 1)
const paginationStart = computed(() => displayedAudits.value.length === 0 ? 0 : currentPage.value * pageSize + 1)
const paginationEnd = computed(() => Math.min((currentPage.value + 1) * pageSize, displayedAudits.value.length))

// 통계 계산
const errorRate = computed(() => {
  if (!stats.value || !stats.value.totalRequests) return '0.00'
  return ((stats.value.errorCount / stats.value.totalRequests) * 100).toFixed(2)
})

const errorRateClass = computed(() => {
  const rate = parseFloat(errorRate.value)
  if (rate < 1) return 'text-green-600'
  if (rate < 5) return 'text-yellow-600'
  return 'text-red-600'
})

const avgExecutionTime = computed(() => {
  if (!stats.value || !stats.value.avgExecutionTime) return '-'
  return stats.value.avgExecutionTime.toFixed(0)
})

// 초기 로드
onMounted(async () => {
  await loadData()
})

// 데이터 로드
async function loadData() {
  loading.value = true
  try {
    const [pageData, statsData] = await Promise.all([
      getAuditsPaged(0, 500), // 최대 500개 로드
      getAuditStats(),
    ])
    audits.value = pageData.audits
    displayedAudits.value = pageData.audits // 초기에는 전체 표시
    stats.value = statsData
  } catch (error) {
    console.error('Failed to load audits:', error)
  } finally {
    loading.value = false
  }
}

// 검색 실행
function applySearch() {
  currentPage.value = 0
  
  let result = audits.value

  // 키워드 검색 (URI, 이메일, IP)
  if (filters.value.keyword.trim()) {
    const keyword = filters.value.keyword.toLowerCase().trim()
    result = result.filter(a => 
      a.requestUri?.toLowerCase().includes(keyword) ||
      a.userEmail?.toLowerCase().includes(keyword) ||
      a.clientIp?.toLowerCase().includes(keyword) ||
      a.serviceName?.toLowerCase().includes(keyword)
    )
  }

  // 서비스 필터
  if (filters.value.serviceName) {
    result = result.filter(a => a.serviceName === filters.value.serviceName)
  }

  // 이벤트 타입 필터
  if (filters.value.eventType) {
    result = result.filter(a => a.eventType === filters.value.eventType)
  }

  // 상태 필터
  if (filters.value.statusFilter === 'success') {
    result = result.filter(a => a.httpStatus >= 200 && a.httpStatus < 400)
  } else if (filters.value.statusFilter === 'error') {
    result = result.filter(a => a.httpStatus >= 400)
  }

  displayedAudits.value = result
}

// 필터 초기화
function resetFilters() {
  filters.value = {
    keyword: '',
    serviceName: '',
    eventType: '',
    statusFilter: 'all',
  }
  currentPage.value = 0
  displayedAudits.value = audits.value // 전체 데이터 표시
}

// 페이지네이션
function prevPage() {
  if (currentPage.value > 0) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
  }
}

// 상세 보기
function showDetail(audit: AuditResponse) {
  selectedAudit.value = audit
}

// 유틸리티 함수들
function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function formatJson(str: string): string {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

function getMethodClass(method: string): string {
  const classes: Record<string, string> = {
    GET: 'bg-green-100 text-green-800',
    POST: 'bg-blue-100 text-blue-800',
    PUT: 'bg-yellow-100 text-yellow-800',
    PATCH: 'bg-orange-100 text-orange-800',
    DELETE: 'bg-red-100 text-red-800',
  }
  return classes[method] || 'bg-gray-100 text-gray-800'
}

function getStatusClass(status: number): string {
  if (status >= 200 && status < 300) return 'bg-green-100 text-green-800'
  if (status >= 300 && status < 400) return 'bg-yellow-100 text-yellow-800'
  if (status >= 400 && status < 500) return 'bg-orange-100 text-orange-800'
  if (status >= 500) return 'bg-red-100 text-red-800'
  return 'bg-gray-100 text-gray-800'
}
</script>
