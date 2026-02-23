<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as notificationApi from '@/api/notification'
import type { Notification } from '@/api/notification'

const router = useRouter()

const notifications = ref<Notification[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const unreadCount = ref(0)

// 알림 목록 조회
async function fetchNotifications(page = 0) {
  try {
    loading.value = true
    error.value = null
    const response: any = await notificationApi.getMyNotifications(page, 20)
    
    // Page 응답 처리
    if (response.content) {
      notifications.value = response.content
      totalPages.value = response.totalPages
      totalElements.value = response.totalElements
      currentPage.value = response.number
      // 읽지 않은 알림 개수 계산
      unreadCount.value = notifications.value.filter(n => !n.isRead).length
    } else if (Array.isArray(response)) {
      notifications.value = response
      unreadCount.value = notifications.value.filter(n => !n.isRead).length
    }
  } catch (e: any) {
    console.error('알림 목록 조회 실패:', e)
    error.value = e.message || '알림을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

// 알림 클릭
async function handleNotificationClick(notification: Notification) {
  if (!notification.isRead) {
    try {
      await notificationApi.markAsRead(notification.id)
      notification.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {
      console.error('읽음 처리 실패:', e)
    }
  }
  
  if (notification.link) {
    router.push(notification.link)
  }
}

// 모두 읽음 처리
async function handleMarkAllAsRead() {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
  } catch (e) {
    console.error('모두 읽음 처리 실패:', e)
  }
}

// 페이지 이동
function goToPage(page: number) {
  if (page >= 0 && page < totalPages.value) {
    fetchNotifications(page)
  }
}

// 시간 포맷
function formatTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '방금 전'
  if (minutes < 60) return `${minutes}분 전`
  if (hours < 24) return `${hours}시간 전`
  if (days < 7) return `${days}일 전`
  
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 알림 타입별 아이콘
function getNotificationIcon(type: string): string {
  const icons: Record<string, string> = {
    DOCUMENT_PASS: '📄✅',
    DOCUMENT_FAIL: '📄❌',
    CODING_TEST_SCHEDULED: '💻📅',
    CODING_TEST_PASS: '💻✅',
    CODING_TEST_FAIL: '💻❌',
    INTERVIEW_SCHEDULED: '🎤📅',
    INTERVIEW_PASS: '🎤✅',
    INTERVIEW_FAIL: '🎤❌',
    FINAL_PASS: '🎉',
    FINAL_FAIL: '😢',
    APPLY_RECEIVED: '📨',
    RESUME_ANALYSIS_COMPLETE: '🤖',
    SYSTEM: '🔔'
  }
  return icons[type] || '🔔'
}

onMounted(() => {
  fetchNotifications()
})
</script>

<template>
  <div class="max-w-4xl mx-auto p-6">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">알림</h1>
        <p class="text-gray-500 mt-1">
          <span v-if="unreadCount > 0" class="text-primary-600 font-medium">읽지 않은 알림 {{ unreadCount }}개</span>
          <span v-if="unreadCount > 0"> · </span>
          <span>전체 {{ totalElements }}개</span>
        </p>
      </div>
      <button
        v-if="notifications.some(n => !n.isRead)"
        @click="handleMarkAllAsRead"
        class="px-4 py-2 text-sm text-primary-600 hover:text-primary-700 hover:bg-primary-50 rounded-lg transition-colors"
      >
        모두 읽음 처리
      </button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="flex justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-primary-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>

    <!-- 에러 -->
    <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
      {{ error }}
      <button @click="fetchNotifications()" class="ml-2 underline">다시 시도</button>
    </div>

    <!-- 알림 없음 -->
    <div v-else-if="notifications.length === 0" class="text-center py-16">
      <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
      </svg>
      <p class="text-gray-500 text-lg">알림이 없습니다</p>
    </div>

    <!-- 알림 목록 -->
    <div v-else class="space-y-3">
      <button
        v-for="notification in notifications"
        :key="notification.id"
        @click="handleNotificationClick(notification)"
        class="w-full text-left p-4 bg-white border rounded-xl hover:shadow-md transition-all"
        :class="{ 
          'border-primary-200 bg-primary-50/50': !notification.isRead,
          'border-gray-200': notification.isRead 
        }"
      >
        <div class="flex items-start gap-4">
          <!-- 아이콘 -->
          <span class="text-2xl">{{ getNotificationIcon(notification.type) }}</span>
          
          <!-- 내용 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <p class="font-semibold text-gray-900">
                {{ notification.title }}
              </p>
              <span 
                v-if="!notification.isRead"
                class="w-2 h-2 bg-primary-500 rounded-full"
              ></span>
            </div>
            <p class="text-gray-600 mt-1">
              {{ notification.message }}
            </p>
            <p class="text-sm text-gray-400 mt-2">
              {{ formatTime(notification.createdAt) }}
            </p>
          </div>

          <!-- 화살표 -->
          <svg v-if="notification.link" class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </button>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-2 mt-8">
      <button
        @click="goToPage(currentPage - 1)"
        :disabled="currentPage === 0"
        class="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
      >
        이전
      </button>
      
      <div class="flex gap-1">
        <button
          v-for="page in Math.min(totalPages, 5)"
          :key="page - 1"
          @click="goToPage(page - 1)"
          class="w-10 h-10 rounded-lg"
          :class="currentPage === page - 1 
            ? 'bg-primary-600 text-white' 
            : 'border border-gray-300 hover:bg-gray-50'"
        >
          {{ page }}
        </button>
      </div>
      
      <button
        @click="goToPage(currentPage + 1)"
        :disabled="currentPage >= totalPages - 1"
        class="px-3 py-2 rounded-lg border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
      >
        다음
      </button>
    </div>
  </div>
</template>
