import { api } from './index'
import type {
  Jobposting,
  JobpostingCreateRequest,
  JobpostingUpdateRequest,
  JobpostingPageResponse,
  JobpostingListResponse,
  JobpostingReadResponse,
  JobpostingReadPageResponse,
  LikeResponse,
  Comment,
  CommentCreateRequest,
  CommentPageResponse,
  HotJobpostingResponse,
} from '@/types/jobposting'

// ============================================
// Jobposting Service - CRUD
// ============================================
export const jobpostingService = {
  async getById(jobpostingId: number): Promise<Jobposting> {
    return api.get(`/api/v1/jobpostings/${jobpostingId}`)
  },

  async getList(boardId: number, page: number, pageSize: number): Promise<JobpostingPageResponse> {
    return api.get('/api/v1/jobpostings', {
      params: { boardId, page, pageSize },
    })
  },

  async getByWriter(writerId: number): Promise<JobpostingListResponse> {
    return api.get(`/api/v1/jobpostings/writers/${writerId}`)
  },

  async getMyJobpostings(): Promise<JobpostingListResponse> {
    return api.get('/api/v1/jobpostings/me')
  },

  async create(request: JobpostingCreateRequest): Promise<Jobposting> {
    return api.post('/api/v1/jobpostings', request)
  },

  async update(jobpostingId: number, request: JobpostingUpdateRequest): Promise<Jobposting> {
    return api.put(`/api/v1/jobpostings/${jobpostingId}`, request)
  },

  async delete(jobpostingId: number): Promise<void> {
    return api.delete(`/api/v1/jobpostings/${jobpostingId}`)
  },
}

// ============================================
// Read Service - 통합 조회 (BFF)
// ============================================
export const readService = {
  // 단일 채용공고 조회 (통계 포함)
  async getById(jobpostingId: number): Promise<JobpostingReadResponse> {
    return api.get(`/api/v1/jobposting-read/${jobpostingId}`)
  },

  // 채용공고 목록 조회 (통계 포함)
  async getList(boardId: number, page: number, pageSize: number): Promise<JobpostingReadPageResponse> {
    return api.get('/api/v1/jobposting-read', {
      params: { boardId, page, pageSize },
    })
  },
}

// ============================================
// Comment Service
// ============================================
export const commentService = {
  async getById(commentId: number): Promise<Comment> {
    return api.get(`/api/v1/comments/${commentId}`)
  },

  async getList(jobpostingId: number, page: number, pageSize: number): Promise<CommentPageResponse> {
    return api.get('/api/v1/comments', {
      params: { jobpostingId, page, pageSize },
    })
  },

  async create(request: CommentCreateRequest): Promise<Comment> {
    return api.post('/api/v1/comments', request)
  },

  async delete(commentId: number): Promise<void> {
    return api.delete(`/api/v1/comments/${commentId}`)
  },
}

// ============================================
// View Service
// ============================================
export const viewService = {
  // 조회수 증가 (인증 필요)
  async increaseViewCount(jobpostingId: number): Promise<number> {
    return api.post(`/api/v1/jobposting-views/jobpostings/${jobpostingId}`)
  },

  // 조회수 조회 (인증 불필요)
  async getViewCount(jobpostingId: number): Promise<number> {
    return api.get(`/api/v1/jobposting-views/jobpostings/${jobpostingId}/count`)
  },
}

// ============================================
// Like Service
// ============================================
export const likeService = {
  // 좋아요 상태 조회 (인증 필요)
  async getLikeStatus(jobpostingId: number): Promise<LikeResponse> {
    return api.get(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}`)
  },

  // 좋아요 수 조회 (인증 불필요)
  async getLikeCount(jobpostingId: number): Promise<number> {
    return api.get(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}/count`)
  },

  // 좋아요 (인증 필요)
  async like(jobpostingId: number): Promise<void> {
    return api.post(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}`)
  },

  // 좋아요 취소 (인증 필요)
  async unlike(jobpostingId: number): Promise<void> {
    return api.delete(`/api/v1/jobposting-likes/jobpostings/${jobpostingId}`)
  },
}

// ============================================
// Hot Service - 인기 공고
// ============================================
export const hotService = {
  // 오늘의 인기 공고 TOP 10
  async getHotToday(): Promise<HotJobpostingResponse[]> {
    return api.get('/api/v1/hot-jobpostings/today')
  },

  // 특정 날짜의 인기 공고
  async getHotByDate(dateStr: string): Promise<HotJobpostingResponse[]> {
    return api.get(`/api/v1/hot-jobpostings/date/${dateStr}`)
  },
}