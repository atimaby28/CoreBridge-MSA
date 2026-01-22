import axios from 'axios'
import type { BaseResponse } from '@/types/common'
import type {
  Jobposting,
  JobpostingDetail,
  JobpostingCreateRequest,
  JobpostingUpdateRequest,
  JobpostingPageResponse,
  JobpostingListResponse,
  LikeResponse,
  Comment,
  CommentCreateRequest,
  CommentPageResponse,
} from '@/types/jobposting'

// ============================================
// API Base URL
// ============================================
const JOBPOSTING_API_URL = import.meta.env.VITE_JOBPOSTING_API_URL || 'http://localhost:8002'
const LIKE_API_URL = import.meta.env.VITE_LIKE_API_URL || 'http://localhost:8003'
const VIEW_API_URL = import.meta.env.VITE_VIEW_API_URL || 'http://localhost:8004'
const COMMENT_API_URL = import.meta.env.VITE_COMMENT_API_URL || 'http://localhost:8005'

// ============================================
// Axios 인스턴스 생성
// ============================================
function createApiInstance(baseURL: string) {
  const instance = axios.create({
    baseURL,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true,  // Cookie 자동 전송
  })

  // 응답 인터셉터
  instance.interceptors.response.use(
    (response) => {
      const data = response.data as BaseResponse
      if (data.success) {
        return data.result
      }
      return Promise.reject(new Error(data.message || '요청 실패'))
    },
    (error) => {
      const message = error.response?.data?.message || error.message || '요청 실패'
      return Promise.reject(new Error(message))
    }
  )

  return instance
}

const jobpostingApi = createApiInstance(JOBPOSTING_API_URL)
const likeApi = createApiInstance(LIKE_API_URL)
const viewApi = createApiInstance(VIEW_API_URL)
const commentApi = createApiInstance(COMMENT_API_URL)

// ============================================
// Jobposting Service
// ============================================
export const jobpostingService = {
  // 채용공고 단건 조회
  async getById(jobpostingId: number): Promise<Jobposting> {
    return jobpostingApi.get(`/api/v1/jobpostings/${jobpostingId}`)
  },

  // 채용공고 목록 조회 (페이징)
  async getList(boardId: number, page: number, pageSize: number): Promise<JobpostingPageResponse> {
    return jobpostingApi.get('/api/v1/jobpostings', {
      params: { boardId, page, pageSize },
    })
  },

  // 작성자별 채용공고 조회
  async getByWriter(writerId: number): Promise<JobpostingListResponse> {
    return jobpostingApi.get(`/api/v1/jobpostings/writers/${writerId}`)
  },

  // 내 채용공고 조회
  async getMyJobpostings(): Promise<JobpostingListResponse> {
    return jobpostingApi.get('/api/v1/jobpostings/me')
  },

  // 채용공고 생성
  async create(request: JobpostingCreateRequest): Promise<Jobposting> {
    return jobpostingApi.post('/api/v1/jobpostings', request)
  },

  // 채용공고 수정
  async update(jobpostingId: number, request: JobpostingUpdateRequest): Promise<Jobposting> {
    return jobpostingApi.put(`/api/v1/jobpostings/${jobpostingId}`, request)
  },

  // 채용공고 삭제
  async delete(jobpostingId: number): Promise<void> {
    return jobpostingApi.delete(`/api/v1/jobpostings/${jobpostingId}`)
  },
}

// ============================================
// Like Service
// ============================================
export const likeService = {
  // 좋아요 상태 조회
  async getLikeStatus(jobpostingId: number, userId: number): Promise<LikeResponse> {
    return likeApi.get(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/users/${userId}`)
  },

  // 좋아요 수 조회
  async getLikeCount(jobpostingId: number): Promise<number> {
    return likeApi.get(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/count`)
  },

  // 좋아요
  async like(jobpostingId: number, userId: number): Promise<void> {
    return likeApi.post(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/users/${userId}`)
  },

  // 좋아요 취소
  async unlike(jobpostingId: number, userId: number): Promise<void> {
    return likeApi.delete(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/users/${userId}`)
  },
}

// ============================================
// View Service
// ============================================
export const viewService = {
  // 조회수 증가
  async increaseViewCount(jobpostingId: number, userId: number): Promise<number> {
    return viewApi.post(`/api/v1/jobposting-views/jobpostings/${jobpostingId}/users/${userId}`)
  },

  // 조회수 조회
  async getViewCount(jobpostingId: number): Promise<number> {
    return viewApi.get(`/api/v1/jobposting-views/jobpostings/${jobpostingId}/count`)
  },
}

// ============================================
// Comment Service
// ============================================
export const commentService = {
  // 댓글 단건 조회
  async getById(commentId: number): Promise<Comment> {
    return commentApi.get(`/api/v1/comments/${commentId}`)
  },

  // 댓글 목록 조회 (페이징)
  async getList(jobpostingId: number, page: number, pageSize: number): Promise<CommentPageResponse> {
    return commentApi.get('/api/v1/comments', {
      params: { jobpostingId, page, pageSize },
    })
  },

  // 댓글 생성
  async create(request: CommentCreateRequest): Promise<Comment> {
    return commentApi.post('/api/v1/comments', request)
  },

  // 댓글 삭제
  async delete(commentId: number): Promise<void> {
    return commentApi.delete(`/api/v1/comments/${commentId}`)
  },
}

// ============================================
// 통합 조회 (상세 페이지용)
// ============================================
export const jobpostingDetailService = {
  // 채용공고 상세 조회 (조회수, 좋아요 수 포함)
  async getDetail(jobpostingId: number, userId?: number): Promise<JobpostingDetail> {
    // 기본 정보 조회
    const jobposting = await jobpostingService.getById(jobpostingId)

    // 조회수 증가 및 조회 (로그인 시)
    let viewCount = 0
    if (userId) {
      try {
        viewCount = await viewService.increaseViewCount(jobpostingId, userId)
      } catch {
        viewCount = await viewService.getViewCount(jobpostingId)
      }
    } else {
      try {
        viewCount = await viewService.getViewCount(jobpostingId)
      } catch {
        viewCount = 0
      }
    }

    // 좋아요 수 조회
    let likeCount = 0
    let isLiked = false
    try {
      likeCount = await likeService.getLikeCount(jobpostingId)
      if (userId) {
        const likeStatus = await likeService.getLikeStatus(jobpostingId, userId)
        isLiked = likeStatus.liked
      }
    } catch {
      likeCount = 0
    }

    // 댓글 수 조회 (첫 페이지 조회로 count 획득)
    let commentCount = 0
    try {
      const comments = await commentService.getList(jobpostingId, 1, 1)
      commentCount = comments.commentCount
    } catch {
      commentCount = 0
    }

    return {
      ...jobposting,
      viewCount,
      likeCount,
      commentCount,
      isLiked,
    }
  },
}
