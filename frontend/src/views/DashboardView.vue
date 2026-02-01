<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { adminUserService } from '@/api/user'
import { getAuditStats, getRecentAudits, type AuditStatsResponse, type AuditResponse } from '@/api/audit'
import { hotService, readService } from '@/api/jobposting'
import { getUserApplyStats } from '@/api/apply'
import { getMySchedules } from '@/api/schedule'
import type { HotJobpostingResponse, JobpostingReadResponse } from '@/types/jobposting'

const router = useRouter()
const authStore = useAuthStore()

// Admin 사용자 통계 데이터
const userStats = ref({
  totalUsers: 0,
  activeUsers: 0,
  blockedUsers: 0,
  adminCount: 0,
  companyCount: 0,
  userCount: 0,
})

// Admin 감사 로그 통계
const auditStats = ref<AuditStatsResponse | null>(null)
const recentAudits = ref<AuditResponse[]>([])

// 인기 공고 & 최신 공고
const hotJobpostings = ref<HotJobpostingResponse[]>([])
const recentJobpostings = ref<JobpostingReadResponse[]>([])

// User/Company 통계
const stats = ref({
  totalApplications: 0,
  pendingApplications: 0,
  interviewsThisWeek: 0,
  passRate: 0,
})

const loading = ref(true)
const statsLoading = ref(false)
const auditLoading = ref(false)

// 역할에 따른 대시보드 타이틀
const dashboardTitle = computed(() => {
  if (authStore.isAdmin) return '관리자 대시보드'
  if (authStore.isCompany) return '채용 관리 대시보드'
  return '취업 현황 대시보드'
})

const welcomeMessage = computed(() => {
  if (authStore.isAdmin) return '시스템 현황을 확인하세요.'
  if (authStore.isCompany) return '채용 진행 현황을 확인하세요.'
  return '오늘의 채용 현황을 확인해보세요.'
})

// 에러율 계산
const errorRate = computed(() => {
  if (!auditStats.value || !auditStats.value.totalRequests) return '0.0'
  return ((auditStats.value.errorCount / auditStats.value.totalRequests) * 100).toFixed(1)
})

// Admin 통계 로드
async function loadUserStats() {
  if (!authStore.isAdmin) return
  
  statsLoading.value = true
  try {
    const data = await adminUserService.getStats()
    userStats.value = data
  } catch (e) {
    console.error('통계 로드 실패:', e)
  } finally {
    statsLoading.value = false
  }
}

// Admin 감사 로그 통계 로드
async function loadAuditStats() {
  if (!authStore.isAdmin) return
  
  auditLoading.value = true
  try {
    const [stats, recent] = await Promise.all([
      getAuditStats(),
      getRecentAudits(10),
    ])
    auditStats.value = stats
    recentAudits.value = recent.audits
  } catch (e) {
    console.error('감사 로그 통계 로드 실패:', e)
  } finally {
    auditLoading.value = false
  }
}

// User 통계 로드 (지원 현황 + 면접)
async function loadUserDashboardStats() {
  if (!authStore.isUser || !authStore.userId) return

  try {
    // 지원 통계
    const applyStats = await getUserApplyStats(authStore.userId)
    stats.value.totalApplications = applyStats.totalProcesses ?? 0
    stats.value.pendingApplications = applyStats.pendingProcesses ?? 0
    stats.value.passRate = applyStats.passRate ?? 0

    // 이번 주 면접 (Schedule)
    try {
      const scheduleData = await getMySchedules()
      const now = new Date()
      const weekStart = new Date(now)
      weekStart.setDate(now.getDate() - now.getDay())
      weekStart.setHours(0, 0, 0, 0)
      const weekEnd = new Date(weekStart)
      weekEnd.setDate(weekStart.getDate() + 7)

      stats.value.interviewsThisWeek = (scheduleData.schedules || []).filter(s => {
        const scheduleDate = new Date(s.startTime || s.scheduledAt)
        return scheduleDate >= weekStart && scheduleDate < weekEnd
      }).length
    } catch {
      // 면접 조회 실패해도 무시
    }
  } catch (e) {
    console.error('사용자 통계 로드 실패:', e)
  }
}

// 인기 공고 로드
async function loadHotJobpostings() {
  try {
    hotJobpostings.value = await hotService.getHotToday()
  } catch (e) {
    console.error('인기 공고 로드 실패:', e)
    hotJobpostings.value = []
  }
}

// 최신 공고 로드
async function loadRecentJobpostings() {
  try {
    const response = await readService.getList(1, 1, 5)
    recentJobpostings.value = response.jobpostings || []
  } catch (e) {
    console.error('최신 공고 로드 실패:', e)
    recentJobpostings.value = []
  }
}

// 공고 상세 페이지로 이동
function goToJobposting(jobpostingId: number) {
  router.push(`/jobpostings/${jobpostingId}`)
}

// 날짜 포맷
function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
  })
}

// HTTP 메서드 색상
function getMethodClass(method: string): string {
  const classes: Record<string, string> = {
    GET: 'bg-green-100 text-green-700',
    POST: 'bg-blue-100 text-blue-700',
    PUT: 'bg-yellow-100 text-yellow-700',
    PATCH: 'bg-orange-100 text-orange-700',
    DELETE: 'bg-red-100 text-red-700',
  }
  return classes[method] || 'bg-gray-100 text-gray-700'
}

// HTTP 상태 색상
function getStatusClass(status: number): string {
  if (status >= 200 && status < 300) return 'text-green-600'
  if (status >= 300 && status < 400) return 'text-yellow-600'
  if (status >= 400) return 'text-red-600'
  return 'text-gray-600'
}

onMounted(async () => {
  try {
    if (authStore.isAdmin) {
      await Promise.all([loadUserStats(), loadAuditStats()])
    } else {
      const promises: Promise<void>[] = [loadHotJobpostings(), loadRecentJobpostings()]

      // User: 지원 통계 로드
      if (authStore.isUser) {
        promises.push(loadUserDashboardStats())
      }

      await Promise.all(promises)
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <!-- 환영 메시지 -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">
        안녕하세요, {{ authStore.user?.nickname || '사용자' }}님! 👋
      </h1>
      <p class="text-gray-500 mt-1">{{ welcomeMessage }}</p>
    </div>

    <!-- ========== 관리자 대시보드 ========== -->
    <template v-if="authStore.isAdmin">
      <!-- 사용자 통계 카드 -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">전체 사용자</p>
              <p class="text-2xl font-bold text-gray-900">{{ userStats.totalUsers }}</p>
            </div>
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">활성 사용자</p>
              <p class="text-2xl font-bold text-green-600">{{ userStats.activeUsers }}</p>
            </div>
            <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">기업 회원</p>
              <p class="text-2xl font-bold text-purple-600">{{ userStats.companyCount }}</p>
            </div>
            <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">차단 사용자</p>
              <p class="text-2xl font-bold text-red-600">{{ userStats.blockedUsers }}</p>
            </div>
            <div class="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 시스템 모니터링 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
        <div class="card">
          <p class="text-sm text-gray-500">총 요청 수</p>
          <p class="text-2xl font-bold text-gray-900">{{ auditStats?.totalRequests ?? 0 }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">평균 응답시간</p>
          <p class="text-2xl font-bold text-blue-600">{{ auditStats?.avgExecutionTime?.toFixed(0) ?? 0 }}ms</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">에러 수</p>
          <p class="text-2xl font-bold text-red-600">{{ auditStats?.errorCount ?? 0 }}</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">에러율</p>
          <p class="text-2xl font-bold" :class="Number(errorRate) > 5 ? 'text-red-600' : 'text-green-600'">{{ errorRate }}%</p>
        </div>
      </div>

      <!-- 최근 감사 로그 -->
      <div class="card">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold text-gray-900">📋 최근 API 요청</h2>
          <router-link to="/admin/audit" class="text-sm text-primary-600 hover:text-primary-700">
            전체보기 →
          </router-link>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-gray-500 border-b">
                <th class="pb-3 font-medium">시간</th>
                <th class="pb-3 font-medium">서비스</th>
                <th class="pb-3 font-medium">메서드</th>
                <th class="pb-3 font-medium">URI</th>
                <th class="pb-3 font-medium">상태</th>
                <th class="pb-3 font-medium">응답시간</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="audit in recentAudits" :key="audit.auditId" class="hover:bg-gray-50">
                <td class="py-3 text-gray-500">{{ formatDateTime(audit.createdAt) }}</td>
                <td class="py-3">
                  <span class="px-2 py-0.5 text-xs rounded bg-blue-100 text-blue-700">
                    {{ audit.serviceName }}
                  </span>
                </td>
                <td class="py-3">
                  <span :class="getMethodClass(audit.httpMethod)" class="px-2 py-0.5 text-xs rounded font-medium">
                    {{ audit.httpMethod }}
                  </span>
                </td>
                <td class="py-3 text-gray-600 max-w-[200px] truncate" :title="audit.requestUri">
                  {{ audit.requestUri }}
                </td>
                <td class="py-3 font-medium" :class="getStatusClass(audit.httpStatus)">
                  {{ audit.httpStatus }}
                </td>
                <td class="py-3 text-gray-500">{{ audit.executionTime }}ms</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ========== User / Company 대시보드 ========== -->
    <template v-else>
      <!-- User 전용: 통계 카드 -->
      <div v-if="authStore.isUser" class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">총 지원</p>
              <p class="text-2xl font-bold text-gray-900">{{ stats.totalApplications }}</p>
            </div>
            <div class="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">진행 중</p>
              <p class="text-2xl font-bold text-yellow-600">{{ stats.pendingApplications }}</p>
            </div>
            <div class="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">이번 주 면접</p>
              <p class="text-2xl font-bold text-blue-600">{{ stats.interviewsThisWeek }}</p>
            </div>
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">합격률</p>
              <p class="text-2xl font-bold text-green-600">{{ stats.passRate }}%</p>
            </div>
            <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- Company 전용: 통계 카드 -->
      <div v-if="authStore.isCompany" class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">진행중 공고</p>
              <p class="text-2xl font-bold text-gray-900">0</p>
            </div>
            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">총 지원자</p>
              <p class="text-2xl font-bold text-green-600">0</p>
            </div>
            <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">면접 예정</p>
              <p class="text-2xl font-bold text-yellow-600">0</p>
            </div>
            <div class="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">채용 완료</p>
              <p class="text-2xl font-bold text-purple-600">0</p>
            </div>
            <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 인기 채용공고 + 최신 채용공고 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 인기 채용공고 -->
        <div class="card">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-gray-900">🔥 인기 채용공고</h2>
            <router-link to="/jobpostings" class="text-sm text-primary-600 hover:text-primary-700">
              전체 보기 →
            </router-link>
          </div>
          
          <div v-if="loading" class="py-8 text-center text-gray-500">로딩중...</div>
          <div v-else-if="hotJobpostings.length === 0" class="py-8 text-center text-gray-500">
            인기 공고가 없습니다
          </div>
          <div v-else class="space-y-1">
            <div
              v-for="(job, index) in hotJobpostings.slice(0, 5)"
              :key="job.jobpostingId"
              @click="goToJobposting(job.jobpostingId)"
              class="flex items-center space-x-3 p-3 hover:bg-gray-50 rounded-lg transition-colors cursor-pointer"
            >
              <span class="font-bold text-lg text-primary-600 w-6">{{ index + 1 }}</span>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-900 truncate">{{ job.title }}</p>
                <p class="text-sm text-gray-500">
                  조회 {{ job.viewCount }} · 좋아요 {{ job.likeCount }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- 최신 채용공고 -->
        <div class="card">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-gray-900">📝 최신 채용공고</h2>
            <router-link to="/jobpostings" class="text-sm text-primary-600 hover:text-primary-700">
              전체 보기 →
            </router-link>
          </div>
          
          <div v-if="loading" class="py-8 text-center text-gray-500">로딩중...</div>
          <div v-else-if="recentJobpostings.length === 0" class="py-8 text-center text-gray-500">
            채용공고가 없습니다
          </div>
          <div v-else class="space-y-1">
            <div
              v-for="job in recentJobpostings"
              :key="job.jobpostingId"
              @click="goToJobposting(job.jobpostingId)"
              class="flex items-center space-x-3 p-3 hover:bg-gray-50 rounded-lg transition-colors cursor-pointer"
            >
              <div class="w-12 h-12 bg-gray-100 rounded-lg flex flex-col items-center justify-center flex-shrink-0">
                <span class="text-xs text-gray-500 font-medium">{{ formatDate(job.createdAt) }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-900 truncate">{{ job.title }}</p>
                <p class="text-sm text-gray-500">{{ job.nickname || '익명' }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 안내 메시지 -->
      <div class="card bg-blue-50 border-blue-200 mt-6">
        <div class="flex items-start space-x-3">
          <svg class="w-6 h-6 text-blue-600 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <h3 class="font-medium text-blue-900">
              {{ authStore.isCompany ? '채용 관리 기능 준비 중' : '취업 지원 기능 준비 중' }}
            </h3>
            <p class="text-sm text-blue-700 mt-1">
              {{ authStore.isCompany 
                ? '채용공고 등록, 지원자 관리 기능이 곧 추가됩니다.'
                : '채용공고 검색, 지원 관리 기능이 곧 추가됩니다.' 
              }}
            </p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
