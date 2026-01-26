import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Notification } from '@/api/notification'
import * as notificationApi from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  // ============================================
  // State
  // ============================================
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ============================================
  // Getters
  // ============================================
  const hasUnread = computed(() => unreadCount.value > 0)
  
  const recentNotifications = computed(() => 
    notifications.value.slice(0, 5)
  )

  // ============================================
  // Actions
  // ============================================

  /**
   * 읽지 않은 알림 개수 조회
   */
  async function fetchUnreadCount() {
    try {
      const response: any = await notificationApi.getUnreadCount()
      unreadCount.value = response.count ?? 0
    } catch (e) {
      console.error('읽지 않은 알림 개수 조회 실패:', e)
    }
  }

  /**
   * 최근 알림 조회
   */
  async function fetchRecentNotifications() {
    try {
      loading.value = true
      error.value = null
      const response: any = await notificationApi.getRecentNotifications()
      notifications.value = response ?? []
      
      // 정확한 읽지 않은 알림 개수는 별도 API로 조회
      await fetchUnreadCount()
    } catch (e) {
      console.error('최근 알림 조회 실패:', e)
      error.value = '알림을 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  /**
   * 알림 읽음 처리
   */
  async function markAsRead(id: number) {
    try {
      await notificationApi.markAsRead(id)
      
      // 로컬 상태 업데이트
      const notification = notifications.value.find(n => n.id === id)
      if (notification && !notification.isRead) {
        notification.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (e) {
      console.error('알림 읽음 처리 실패:', e)
    }
  }

  /**
   * 모든 알림 읽음 처리
   */
  async function markAllAsRead() {
    try {
      await notificationApi.markAllAsRead()
      
      // 로컬 상태 업데이트
      notifications.value.forEach(n => n.isRead = true)
      unreadCount.value = 0
    } catch (e) {
      console.error('모든 알림 읽음 처리 실패:', e)
    }
  }

  return {
    // State
    notifications,
    unreadCount,
    loading,
    error,
    
    // Getters
    hasUnread,
    recentNotifications,
    
    // Actions
    fetchUnreadCount,
    fetchRecentNotifications,
    markAsRead,
    markAllAsRead
  }
})
