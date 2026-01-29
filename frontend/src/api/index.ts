import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { BaseResponse } from '@/types'

// ============================================
// Gateway 단일 진입점
// ============================================
const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8000'

// Axios 인스턴스 생성 (Gateway 통합)
function createApiInstance(): AxiosInstance {
  const instance = axios.create({
    baseURL: GATEWAY_URL,
    timeout: 10000,
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

      // 401 에러 && 재시도 안 한 요청 && refresh 요청이 아닌 경우
      if (error.response?.status === 401 && 
          !originalRequest._retry && 
          !originalRequest.url?.includes('/refresh')) {
        
        originalRequest._retry = true

        try {
          // 토큰 갱신 요청 (Gateway 경유)
          await axios.post(
            `${GATEWAY_URL}/api/v1/users/refresh`,
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