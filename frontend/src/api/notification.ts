import { notificationApi } from './index'

export interface Notification {
  id: number
  type: string
  typeDescription: string
  title: string
  message: string
  link: string | null
  isRead: boolean
  relatedId: number | null
  relatedType: string | null
  createdAt: string
}

export interface NotificationPage {
  content: Notification[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface UnreadCount {
  count: number
}

// 내 알림 목록 조회
export const getMyNotifications = (page = 0, size = 20) =>
  notificationApi.get<NotificationPage>('/api/v1/notifications', {
    params: { page, size }
  })

// 읽지 않은 알림만 조회
export const getUnreadNotifications = (page = 0, size = 20) =>
  notificationApi.get<NotificationPage>('/api/v1/notifications/unread', {
    params: { page, size }
  })

// 읽지 않은 알림 개수
export const getUnreadCount = () =>
  notificationApi.get<UnreadCount>('/api/v1/notifications/unread-count')

// 최근 알림 10개 조회
export const getRecentNotifications = () =>
  notificationApi.get<Notification[]>('/api/v1/notifications/recent')

// 알림 단건 읽음 처리
export const markAsRead = (id: number) =>
  notificationApi.patch<boolean>(`/api/v1/notifications/${id}/read`)

// 모든 알림 읽음 처리
export const markAllAsRead = () =>
  notificationApi.patch<number>('/api/v1/notifications/read-all')
