// API 공통 응답
export interface BaseResponse<T = any> {
  success: boolean
  code: number
  message: string
  result: T
}

// 페이징 요청
export interface PageRequest {
  page?: number
  size?: number
}

// 페이징 응답
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
  hasPrevious: boolean
}
