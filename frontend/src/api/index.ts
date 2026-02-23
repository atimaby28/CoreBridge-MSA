import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { BaseResponse } from '@/types'

// ============================================
// Gateway 단일 진입점
// ============================================
const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8000'

// ============================================
// Refresh Token 동시 호출 방지
// ============================================
let isRefreshing = false
let failedQueue: Array<{
  resolve: (value?: unknown) => void
  reject: (reason?: unknown) => void
}> = []

function processQueue(error: unknown) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve()
    }
  })
  failedQueue = []
}

// Axios 인스턴스 생성 (Gateway 통합)
function createApiInstance(): AxiosInstance {
  const instance = axios.create({
    baseURL: GATEWAY_URL,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true,  // 🍪 Cookie 자동 전송
  })

  // 요청 인터셉터
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

      // refresh 요청 자체가 실패한 경우 → 바로 reject (무한 루프 차단)
      if (originalRequest.url?.includes('/refresh')) {
        return Promise.reject(error)
      }

      // 401 에러 && 재시도 안 한 요청
      if (error.response?.status === 401 && !originalRequest._retry) {
        // 이미 refresh 진행 중이면 큐에 대기
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          }).then(() => instance(originalRequest))
            .catch((err) => Promise.reject(err))
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          // 토큰 갱신 요청 (Gateway 경유)
          await axios.post(
            `${GATEWAY_URL}/api/v1/users/refresh`,
            {},
            { withCredentials: true }
          )

          // 대기 중인 요청 모두 재시도
          processQueue(null)

          // 원래 요청 재시도
          return instance(originalRequest)
        } catch (refreshError) {
          // 대기 중인 요청 모두 실패 처리
          processQueue(refreshError)

          // 이미 로그인 페이지면 리다이렉트 안 함 (무한 루프 차단)
          if (!window.location.pathname.startsWith('/auth')) {
            window.location.href = '/auth/login'
          }
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      // 에러 메시지 추출
      const message = error.response?.data?.message || error.message || '요청 실패'
      return Promise.reject(new Error(message))
    }
  )

  return instance
}

// 단일 API 인스턴스 (모든 서비스 공용)
export const api = createApiInstance()

// 하위 호환성을 위한 별칭 (기존 코드 호환)
export const userApi = api
export const jobpostingApi = api
export const commentApi = api
export const viewApi = api
export const likeApi = api
export const hotApi = api
export const readApi = api
export const resumeApi = api
export const applyApi = api
export const notificationApi = api
export const scheduleApi = api
export const auditApi = api