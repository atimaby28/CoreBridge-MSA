export interface Notification {
  id: number
  type: NotificationType
  typeDescription: string
  title: string
  message: string
  link: string | null
  isRead: boolean
  relatedId: number | null
  relatedType: string | null
  createdAt: string
}

export type NotificationType =
  | 'PROCESS_UPDATE'
  | 'DOCUMENT_PASS'
  | 'DOCUMENT_FAIL'
  | 'CODING_TEST_SCHEDULED'
  | 'CODING_TEST_PASS'
  | 'CODING_TEST_FAIL'
  | 'INTERVIEW_SCHEDULED'
  | 'INTERVIEW_PASS'
  | 'INTERVIEW_FAIL'
  | 'FINAL_PASS'
  | 'FINAL_FAIL'
  | 'APPLY_RECEIVED'
  | 'APPLY_CANCELLED'
  | 'RESUME_ANALYSIS_COMPLETE'
  | 'SYSTEM'

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
