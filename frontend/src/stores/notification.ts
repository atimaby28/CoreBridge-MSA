import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Notification } from '@/api/notification'
import * as notificationApi from '@/api/notification'
import { useAuthStore } from '@/stores/auth'

const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8000'

export const useNotificationStore = defineStore('notification', () => {
  // ============================================
  // State
  // ============================================
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const sseConnected = ref(false)

  let eventSource: EventSource | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  // ============================================
  // Getters
  // ============================================
  const hasUnread = computed(() => unreadCount.value > 0)

  const recentNotifications = computed(() =>
    notifications.value.slice(0, 5)
  )

  // ============================================
  // SSE 연결 관리
  // ============================================

  /**
   * SSE 구독 시작 — 로그인 후 호출
   */
  function connectSSE() {
    // 이미 연결 중이면 스킵
    if (sseConnected.value && eventSource) {
      return
    }

    if (eventSource) {
      eventSource.close()
      eventSource = null
    }

    // userId 확인
    const authStore = useAuthStore()
    const userId = authStore.user?.userId
    if (!userId) {
      console.warn('[SSE] userId 없음 — 연결 스킵')
      return
    }

    try {
      // 개발: Vite proxy(/sse → notification:8010) — Gateway 버퍼링 회피
      // 프로덕션: Gateway 경유
      // notification 서비스에 직접 연결 (프록시 없이)
      const sseUrl = `http://localhost:8010/api/v1/notifications/subscribe?userId=${userId}`

      eventSource = new EventSource(sseUrl)

      eventSource.addEventListener('connect', () => {
        sseConnected.value = true
        console.log('[SSE] 연결 성공')
        // 연결 후 초기 데이터 로드
        fetchUnreadCount()
        fetchRecentNotifications()
      })

      eventSource.addEventListener('notification', (event: MessageEvent) => {
        try {
          const notification: Notification = JSON.parse(event.data)
          // 목록 맨 앞에 추가
          notifications.value.unshift(notification)
          // 5개까지만 유지 (드롭다운용)
          if (notifications.value.length > 10) {
            notifications.value = notifications.value.slice(0, 10)
          }
          unreadCount.value++
          console.log('[SSE] 새 알림 수신:', notification.title)
        } catch (e) {
          console.error('[SSE] 알림 파싱 실패:', e)
        }
      })

      eventSource.onerror = () => {
        // EventSource가 자동 재연결 시도 중이면 (readyState=CONNECTING) 무시
        if (eventSource && eventSource.readyState === EventSource.CONNECTING) {
          console.debug('[SSE] 재연결 시도 중...')
          return
        }

        sseConnected.value = false
        console.warn('[SSE] 연결 끊김 — 5초 후 재연결')
        eventSource?.close()
        eventSource = null

        // 로그아웃 상태면 재연결 안 함
        const authStore = useAuthStore()
        if (authStore.isVisitor) return

        // 자동 재연결
        if (reconnectTimer) clearTimeout(reconnectTimer)
        reconnectTimer = setTimeout(() => {
          connectSSE()
        }, 5000)
      }
    } catch (e) {
      console.error('[SSE] 연결 실패:', e)
    }
  }

  /**
   * SSE 구독 해제 — 로그아웃 시 호출
   */
  function disconnectSSE() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    sseConnected.value = false
    console.log('[SSE] 연결 해제')
  }

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
    sseConnected,

    // Getters
    hasUnread,
    recentNotifications,

    // SSE
    connectSSE,
    disconnectSSE,

    // Actions
    fetchUnreadCount,
    fetchRecentNotifications,
    markAsRead,
    markAllAsRead
  }
})
