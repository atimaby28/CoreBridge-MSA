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
const COMMENT_API_URL = import.meta.env.VITE_COMMENT_API_URL || 'http://localhost:8003'
const VIEW_API_URL = import.meta.env.VITE_VIEW_API_URL || 'http://localhost:8004'
const LIKE_API_URL = import.meta.env.VITE_LIKE_API_URL || 'http://localhost:8005'
const HOT_API_URL = import.meta.env.VITE_HOT_API_URL || 'http://localhost:8006'
const READ_API_URL = import.meta.env.VITE_READ_API_URL || 'http://localhost:8007'

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
const commentApi = createApiInstance(COMMENT_API_URL)
const viewApi = createApiInstance(VIEW_API_URL)
const likeApi = createApiInstance(LIKE_API_URL)
const hotApi = createApiInstance(HOT_API_URL)
const readApi = createApiInstance(READ_API_URL)

// ============================================
// Jobposting Service (8002)
// ============================================
export const jobpostingService = {
  async getById(jobpostingId: number): Promise<Jobposting> {
    return jobpostingApi.get(`/api/v1/jobpostings/${jobpostingId}`)
  },

  async getList(boardId: number, page: number, pageSize: number): Promise<JobpostingPageResponse> {
    return jobpostingApi.get('/api/v1/jobpostings', {
      params: { boardId, page, pageSize },
    })
  },

  async getByWriter(writerId: number): Promise<JobpostingListResponse> {
    return jobpostingApi.get(`/api/v1/jobpostings/writers/${writerId}`)
  },

  async getMyJobpostings(): Promise<JobpostingListResponse> {
    return jobpostingApi.get('/api/v1/jobpostings/me')
  },

  async create(request: JobpostingCreateRequest): Promise<Jobposting> {
    return jobpostingApi.post('/api/v1/jobpostings', request)
  },

  async update(jobpostingId: number, request: JobpostingUpdateRequest): Promise<Jobposting> {
    return jobpostingApi.put(`/api/v1/jobpostings/${jobpostingId}`, request)
  },

  async delete(jobpostingId: number): Promise<void> {
    return jobpostingApi.delete(`/api/v1/jobpostings/${jobpostingId}`)
  },
}

// ============================================
// Comment Service (8003)
// ============================================
export const commentService = {
  async getById(commentId: number): Promise<Comment> {
    return commentApi.get(`/api/v1/comments/${commentId}`)
  },

  async getList(jobpostingId: number, page: number, pageSize: number): Promise<CommentPageResponse> {
    return commentApi.get('/api/v1/comments', {
      params: { jobpostingId, page, pageSize },
    })
  },

  async create(request: CommentCreateRequest): Promise<Comment> {
    return commentApi.post('/api/v1/comments', request)
  },

  async delete(commentId: number): Promise<void> {
    return commentApi.delete(`/api/v1/comments/${commentId}`)
  },
}

// ============================================
// View Service (8004)
// ============================================
export const viewService = {
  // 조회수 증가 (인증 필요 - Cookie로 userId 전송)
  async increaseViewCount(jobpostingId: number): Promise<number> {
    return viewApi.post(`/api/v1/jobposting-views/jobpostings/${jobpostingId}`)
  },

  // 조회수 조회 (인증 불필요)
  async getViewCount(jobpostingId: number): Promise<number> {
    return viewApi.get(`/api/v1/jobposting-views/jobpostings/${jobpostingId}/count`)
  },
}

// ============================================
// Like Service (8005 - 예정)
// ============================================
export const likeService = {
  async getLikeStatus(jobpostingId: number, userId: number): Promise<LikeResponse> {
    return likeApi.get(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/users/${userId}`)
  },

  async getLikeCount(jobpostingId: number): Promise<number> {
    return likeApi.get(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/count`)
  },

  async like(jobpostingId: number, userId: number): Promise<void> {
    return likeApi.post(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/users/${userId}`)
  },

  async unlike(jobpostingId: number, userId: number): Promise<void> {
    return likeApi.delete(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/users/${userId}`)
  },
}

// ============================================
// Hot Service (8006 - 예정)
// ============================================
export const hotService = {
  async getHotToday(): Promise<Jobposting[]> {
    return hotApi.get('/api/v1/hot-jobpostings/jobpostings/today')
  },

  async getHotByDate(dateStr: string): Promise<Jobposting[]> {
    return hotApi.get(`/api/v1/hot-jobpostings/jobpostings/date/${dateStr}`)
  },
}

// ============================================
// 통합 조회 (상세 페이지용)
// ============================================
export const jobpostingDetailService = {
  async getDetail(jobpostingId: number, userId?: number): Promise<JobpostingDetail> {
    const jobposting = await jobpostingService.getById(jobpostingId)

    // 조회수 증가 및 조회 (로그인 시)
    let viewCount = 0
    if (userId) {
      try {
        viewCount = await viewService.increaseViewCount(jobpostingId)
      } catch {
        try {
          viewCount = await viewService.getViewCount(jobpostingId)
        } catch {
          viewCount = 0
        }
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

    // 댓글 수 조회
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
