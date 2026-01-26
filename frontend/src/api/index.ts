import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { BaseResponse } from '@/types'

// ============================================
// 서비스별 포트 (CoreBridge MSA)
// ============================================
// user:              8001
// jobposting:        8002
// jobposting-comment: 8003
// jobposting-view:   8004
// jobposting-like:   8005
// jobposting-hot:    8006
// jobposting-read:   8007
// resume:            8008
// apply:             8009
// schedule:          8010 (예정)
// notification:      8011 (예정)
// batch:             8012 (예정)
// admin-audit:       8013
// ============================================

const SERVICE_URLS: Record<string, string> = {
  user: import.meta.env.VITE_USER_API_URL || 'http://localhost:8001',
  jobposting: import.meta.env.VITE_JOBPOSTING_API_URL || 'http://localhost:8002',
  comment: import.meta.env.VITE_COMMENT_API_URL || 'http://localhost:8003',
  view: import.meta.env.VITE_VIEW_API_URL || 'http://localhost:8004',
  like: import.meta.env.VITE_LIKE_API_URL || 'http://localhost:8005',
  hot: import.meta.env.VITE_HOT_API_URL || 'http://localhost:8006',
  read: import.meta.env.VITE_READ_API_URL || 'http://localhost:8007',
  resume: import.meta.env.VITE_RESUME_API_URL || 'http://localhost:8008',
  apply: import.meta.env.VITE_APPLY_API_URL || 'http://localhost:8009',
  audit: import.meta.env.VITE_AUDIT_API_URL || 'http://localhost:8013',
}

// Axios 인스턴스 생성
function createApiInstance(serviceName: string): AxiosInstance {
  const instance = axios.create({
    baseURL: SERVICE_URLS[serviceName],
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true,  // 🍪 Cookie 자동 전송
  })

  // 요청 인터셉터 (Authorization 헤더 불필요 - Cookie 사용)
  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      return config
    },
    (error) => Promise.reject(error)
  )

  // 응답 인터셉터
  instance.interceptors.response.use(
    (response: AxiosResponse) => {
      const data = response.data as BaseResponse
      // BaseResponse: { success, code, message, result }
      if (data.success) {
        return data.result
      }
      return Promise.reject(new Error(data.message || '요청 실패'))
    },
    async (error) => {
      const originalRequest = error.config

      // 401 에러 && 재시도 안 한 요청 && refresh 요청이 아닌 경우
      if (error.response?.status === 401 && 
          !originalRequest._retry && 
          !originalRequest.url?.includes('/refresh')) {
        
        originalRequest._retry = true

        try {
          // 토큰 갱신 요청 (Cookie가 자동으로 전송됨)
          await axios.post(
            `${SERVICE_URLS.user}/api/v1/users/refresh`,
            {},
            { withCredentials: true }
          )

          // 원래 요청 재시도
          return instance(originalRequest)
        } catch (refreshError) {
          // 갱신 실패 시 로그인 페이지로
          window.location.href = '/auth/login'
          return Promise.reject(refreshError)
        }
      }

      // 에러 메시지 추출
      const message = error.response?.data?.message || error.message || '요청 실패'
      return Promise.reject(new Error(message))
    }
  )

  return instance
}

// 서비스별 API 인스턴스
export const userApi = createApiInstance('user')
export const jobpostingApi = createApiInstance('jobposting')
export const commentApi = createApiInstance('comment')
export const viewApi = createApiInstance('view')
export const likeApi = createApiInstance('like')
export const hotApi = createApiInstance('hot')
export const readApi = createApiInstance('read')
export const resumeApi = createApiInstance('resume')
export const applyApi = createApiInstance('apply')
export const auditApi = createApiInstance('audit')
