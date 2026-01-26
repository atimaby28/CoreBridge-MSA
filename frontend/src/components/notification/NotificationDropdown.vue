<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'

const router = useRouter()
const notificationStore = useNotificationStore()

const isOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

// 드롭다운 토글
function toggleDropdown() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    notificationStore.fetchRecentNotifications()
  }
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

// 알림 클릭
async function handleNotificationClick(notification: any) {
  if (!notification.isRead) {
    await notificationStore.markAsRead(notification.id)
  }
  
  if (notification.link) {
    router.push(notification.link)
  }
  
  isOpen.value = false
}

// 모두 읽음 처리
async function handleMarkAllAsRead() {
  await notificationStore.markAllAsRead()
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
  
  return date.toLocaleDateString('ko-KR')
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
  document.addEventListener('click', handleClickOutside)
  notificationStore.fetchUnreadCount()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div ref="dropdownRef" class="relative">
    <!-- 알림 버튼 -->
    <button
      @click="toggleDropdown"
      class="relative p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
    >
      <!-- 벨 아이콘 -->
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path 
          stroke-linecap="round" 
          stroke-linejoin="round" 
          stroke-width="2" 
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </svg>
      
      <!-- 배지 -->
      <span 
        v-if="notificationStore.unreadCount > 0"
        class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs font-bold rounded-full flex items-center justify-center"
      >
        {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
      </span>
    </button>

    <!-- 드롭다운 -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-1"
    >
      <div
        v-if="isOpen"
        class="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden z-50"
      >
        <!-- 헤더 -->
        <div class="flex items-center justify-between px-4 py-3 bg-gray-50 border-b">
          <h3 class="font-semibold text-gray-900">알림</h3>
          <button
            v-if="notificationStore.unreadCount > 0"
            @click="handleMarkAllAsRead"
            class="text-sm text-primary-600 hover:text-primary-700"
          >
            모두 읽음
          </button>
        </div>

        <!-- 알림 목록 -->
        <div class="max-h-96 overflow-y-auto">
          <!-- 로딩 -->
          <div v-if="notificationStore.loading" class="p-4 text-center text-gray-500">
            <svg class="animate-spin h-6 w-6 mx-auto text-primary-600" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>

          <!-- 알림 없음 -->
          <div 
            v-else-if="notificationStore.notifications.length === 0" 
            class="p-8 text-center text-gray-500"
          >
            <svg class="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
            </svg>
            <p>새로운 알림이 없습니다</p>
          </div>

          <!-- 알림 아이템 -->
          <div v-else>
            <button
              v-for="notification in notificationStore.notifications"
              :key="notification.id"
              @click="handleNotificationClick(notification)"
              class="w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0"
              :class="{ 'bg-primary-50': !notification.isRead }"
            >
              <div class="flex items-start gap-3">
                <!-- 아이콘 -->
                <span class="text-xl">{{ getNotificationIcon(notification.type) }}</span>
                
                <!-- 내용 -->
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-900 truncate">
                    {{ notification.title }}
                  </p>
                  <p class="text-sm text-gray-600 line-clamp-2">
                    {{ notification.message }}
                  </p>
                  <p class="text-xs text-gray-400 mt-1">
                    {{ formatTime(notification.createdAt) }}
                  </p>
                </div>

                <!-- 읽지 않음 표시 -->
                <span 
                  v-if="!notification.isRead"
                  class="w-2 h-2 bg-primary-500 rounded-full flex-shrink-0 mt-2"
                ></span>
              </div>
            </button>
          </div>
        </div>

        <!-- 푸터 -->
        <div class="px-4 py-3 bg-gray-50 border-t text-center">
          <router-link 
            to="/notifications"
            @click="isOpen = false"
            class="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            전체 알림 보기
          </router-link>
        </div>
      </div>
    </Transition>
  </div>
</template>
