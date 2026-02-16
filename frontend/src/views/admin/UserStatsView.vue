<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { adminUserService } from '@/api/user'
import type { UserStatsResponse } from '@/types'

const stats = ref<UserStatsResponse | null>(null)
const loading = ref(true)
const error = ref('')

// 역할별 비율 계산
const roleDistribution = computed(() => {
  if (!stats.value || stats.value.totalUsers === 0) return []
  const total = stats.value.totalUsers
  return [
    { label: '일반회원', count: stats.value.userCount, color: 'bg-blue-500', pct: Math.round((stats.value.userCount / total) * 100) },
    { label: '기업회원', count: stats.value.companyCount, color: 'bg-emerald-500', pct: Math.round((stats.value.companyCount / total) * 100) },
    { label: '관리자', count: stats.value.adminCount, color: 'bg-purple-500', pct: Math.round((stats.value.adminCount / total) * 100) },
  ]
})

// 상태별 비율
const statusDistribution = computed(() => {
  if (!stats.value || stats.value.totalUsers === 0) return []
  const total = stats.value.totalUsers
  return [
    { label: '활성', count: stats.value.activeUsers, color: 'bg-green-500', pct: Math.round((stats.value.activeUsers / total) * 100) },
    { label: '차단', count: stats.value.blockedUsers, color: 'bg-red-500', pct: Math.round((stats.value.blockedUsers / total) * 100) },
  ]
})

async function fetchStats() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await adminUserService.getStats()
  } catch (e: any) {
    error.value = '통계를 불러오는데 실패했습니다.'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <!-- 헤더 -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">사용자 통계</h1>
      <p class="mt-1 text-sm text-gray-500">전체 사용자 현황 및 역할별 분포를 확인합니다.</p>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="text-center py-20">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600 mx-auto"></div>
      <p class="mt-4 text-gray-500">통계 불러오는 중...</p>
    </div>

    <!-- 에러 -->
    <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
      <p class="text-red-600">{{ error }}</p>
      <button @click="fetchStats" class="mt-3 text-sm text-red-700 underline">다시 시도</button>
    </div>

    <!-- 통계 내용 -->
    <div v-else-if="stats" class="space-y-8">

      <!-- 요약 카드 4개 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">전체 사용자</p>
              <p class="text-3xl font-bold text-gray-900 mt-1">{{ stats.totalUsers }}</p>
            </div>
            <div class="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">활성 사용자</p>
              <p class="text-3xl font-bold text-green-600 mt-1">{{ stats.activeUsers }}</p>
            </div>
            <div class="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">기업 회원</p>
              <p class="text-3xl font-bold text-emerald-600 mt-1">{{ stats.companyCount }}</p>
            </div>
            <div class="w-12 h-12 bg-emerald-100 rounded-xl flex items-center justify-center">
              <svg class="w-6 h-6 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">차단 사용자</p>
              <p class="text-3xl font-bold text-red-600 mt-1">{{ stats.blockedUsers }}</p>
            </div>
            <div class="w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center">
              <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 역할별 분포 & 상태별 분포 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <!-- 역할별 분포 -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-5">👥 역할별 분포</h2>

          <div class="space-y-4">
            <div v-for="item in roleDistribution" :key="item.label" class="space-y-2">
              <div class="flex justify-between text-sm">
                <span class="font-medium text-gray-700">{{ item.label }}</span>
                <span class="text-gray-500">{{ item.count }}명 ({{ item.pct }}%)</span>
              </div>
              <div class="w-full bg-gray-100 rounded-full h-3">
                <div
                  :class="[item.color, 'h-3 rounded-full transition-all duration-500']"
                  :style="{ width: item.pct + '%' }"
                ></div>
              </div>
            </div>
          </div>

          <!-- 범례 -->
          <div class="mt-6 flex flex-wrap gap-4 text-sm">
            <div v-for="item in roleDistribution" :key="'legend-' + item.label" class="flex items-center gap-2">
              <span :class="[item.color, 'w-3 h-3 rounded-full']"></span>
              <span class="text-gray-600">{{ item.label }} {{ item.count }}명</span>
            </div>
          </div>
        </div>

        <!-- 상태별 분포 -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 class="text-lg font-semibold text-gray-900 mb-5">📊 상태별 분포</h2>

          <div class="space-y-4">
            <div v-for="item in statusDistribution" :key="item.label" class="space-y-2">
              <div class="flex justify-between text-sm">
                <span class="font-medium text-gray-700">{{ item.label }}</span>
                <span class="text-gray-500">{{ item.count }}명 ({{ item.pct }}%)</span>
              </div>
              <div class="w-full bg-gray-100 rounded-full h-3">
                <div
                  :class="[item.color, 'h-3 rounded-full transition-all duration-500']"
                  :style="{ width: item.pct + '%' }"
                ></div>
              </div>
            </div>
          </div>

          <!-- 요약 카드 -->
          <div class="mt-6 grid grid-cols-2 gap-4">
            <div class="bg-green-50 rounded-lg p-4 text-center">
              <p class="text-2xl font-bold text-green-700">{{ stats.activeUsers }}</p>
              <p class="text-xs text-green-600 mt-1">활성 사용자</p>
            </div>
            <div class="bg-red-50 rounded-lg p-4 text-center">
              <p class="text-2xl font-bold text-red-700">{{ stats.blockedUsers }}</p>
              <p class="text-xs text-red-600 mt-1">차단 사용자</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 상세 수치 테이블 -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">📋 상세 수치</h2>
        </div>
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">항목</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">수치</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">비율</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr>
              <td class="px-6 py-4 text-sm text-gray-900 font-medium">전체 사용자</td>
              <td class="px-6 py-4 text-sm text-gray-700 text-right">{{ stats.totalUsers }}명</td>
              <td class="px-6 py-4 text-sm text-gray-500 text-right">100%</td>
            </tr>
            <tr>
              <td class="px-6 py-4 text-sm text-gray-900">├ 일반회원 (ROLE_USER)</td>
              <td class="px-6 py-4 text-sm text-blue-600 text-right font-medium">{{ stats.userCount }}명</td>
              <td class="px-6 py-4 text-sm text-gray-500 text-right">{{ stats.totalUsers > 0 ? Math.round((stats.userCount / stats.totalUsers) * 100) : 0 }}%</td>
            </tr>
            <tr>
              <td class="px-6 py-4 text-sm text-gray-900">├ 기업회원 (ROLE_COMPANY)</td>
              <td class="px-6 py-4 text-sm text-emerald-600 text-right font-medium">{{ stats.companyCount }}명</td>
              <td class="px-6 py-4 text-sm text-gray-500 text-right">{{ stats.totalUsers > 0 ? Math.round((stats.companyCount / stats.totalUsers) * 100) : 0 }}%</td>
            </tr>
            <tr>
              <td class="px-6 py-4 text-sm text-gray-900">└ 관리자 (ROLE_ADMIN)</td>
              <td class="px-6 py-4 text-sm text-purple-600 text-right font-medium">{{ stats.adminCount }}명</td>
              <td class="px-6 py-4 text-sm text-gray-500 text-right">{{ stats.totalUsers > 0 ? Math.round((stats.adminCount / stats.totalUsers) * 100) : 0 }}%</td>
            </tr>
            <tr class="bg-green-50">
              <td class="px-6 py-4 text-sm text-green-800 font-medium">활성 사용자</td>
              <td class="px-6 py-4 text-sm text-green-700 text-right font-medium">{{ stats.activeUsers }}명</td>
              <td class="px-6 py-4 text-sm text-green-600 text-right">{{ stats.totalUsers > 0 ? Math.round((stats.activeUsers / stats.totalUsers) * 100) : 0 }}%</td>
            </tr>
            <tr class="bg-red-50">
              <td class="px-6 py-4 text-sm text-red-800 font-medium">차단 사용자</td>
              <td class="px-6 py-4 text-sm text-red-700 text-right font-medium">{{ stats.blockedUsers }}명</td>
              <td class="px-6 py-4 text-sm text-red-600 text-right">{{ stats.totalUsers > 0 ? Math.round((stats.blockedUsers / stats.totalUsers) * 100) : 0 }}%</td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>
</template>