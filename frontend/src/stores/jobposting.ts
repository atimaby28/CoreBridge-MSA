import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { jobpostingService, jobpostingDetailService, likeService, commentService } from '@/api/jobposting'
import type {
  Jobposting,
  JobpostingDetail,
  JobpostingCreateRequest,
  JobpostingUpdateRequest,
  Comment,
  CommentCreateRequest,
  Board,
  DEFAULT_BOARDS,
} from '@/types/jobposting'

export const useJobpostingStore = defineStore('jobposting', () => {
  // ============================================
  // State
  // ============================================
  const jobpostings = ref<Jobposting[]>([])
  const currentJobposting = ref<JobpostingDetail | null>(null)
  const myJobpostings = ref<Jobposting[]>([])
  const comments = ref<Comment[]>([])
  
  const loading = ref(false)
  const error = ref<string | null>(null)
  
  // 페이징
  const currentPage = ref(1)
  const pageSize = ref(10)
  const totalCount = ref(0)
  
  // 현재 게시판
  const currentBoardId = ref(1)
  
  // 댓글 페이징
  const commentPage = ref(1)
  const commentPageSize = ref(20)
  const commentTotalCount = ref(0)

  // ============================================
  // Getters
  // ============================================
  const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value))
  const hasNextPage = computed(() => currentPage.value < totalPages.value)
  const hasPrevPage = computed(() => currentPage.value > 1)
  
  const commentTotalPages = computed(() => Math.ceil(commentTotalCount.value / commentPageSize.value))

  // ============================================
  // Actions - Jobposting
  // ============================================
  
  // 채용공고 목록 조회
  async function fetchJobpostings(boardId?: number, page?: number): Promise<void> {
    loading.value = true
    error.value = null
    
    try {
      if (boardId !== undefined) currentBoardId.value = boardId
      if (page !== undefined) currentPage.value = page
      
      const response = await jobpostingService.getList(
        currentBoardId.value,
        currentPage.value,
        pageSize.value
      )
      
      jobpostings.value = response.jobpostings
      totalCount.value = response.jobpostingCount
    } catch (e) {
      error.value = e instanceof Error ? e.message : '채용공고 목록을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 채용공고 상세 조회
  async function fetchJobposting(jobpostingId: number, userId?: number): Promise<JobpostingDetail> {
    loading.value = true
    error.value = null
    
    try {
      const detail = await jobpostingDetailService.getDetail(jobpostingId, userId)
      currentJobposting.value = detail
      return detail
    } catch (e) {
      error.value = e instanceof Error ? e.message : '채용공고를 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 내 채용공고 조회
  async function fetchMyJobpostings(): Promise<void> {
    loading.value = true
    error.value = null
    
    try {
      const response = await jobpostingService.getMyJobpostings()
      myJobpostings.value = response.jobpostings
    } catch (e) {
      error.value = e instanceof Error ? e.message : '내 채용공고를 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 채용공고 생성
  async function createJobposting(request: JobpostingCreateRequest): Promise<Jobposting> {
    loading.value = true
    error.value = null
    
    try {
      const jobposting = await jobpostingService.create(request)
      // 목록 갱신
      await fetchJobpostings()
      return jobposting
    } catch (e) {
      error.value = e instanceof Error ? e.message : '채용공고 등록에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 채용공고 수정
  async function updateJobposting(jobpostingId: number, request: JobpostingUpdateRequest): Promise<Jobposting> {
    loading.value = true
    error.value = null
    
    try {
      const jobposting = await jobpostingService.update(jobpostingId, request)
      // 현재 상세 갱신
      if (currentJobposting.value?.jobpostingId === jobpostingId) {
        currentJobposting.value = {
          ...currentJobposting.value,
          ...jobposting,
        }
      }
      return jobposting
    } catch (e) {
      error.value = e instanceof Error ? e.message : '채용공고 수정에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 채용공고 삭제
  async function deleteJobposting(jobpostingId: number): Promise<void> {
    loading.value = true
    error.value = null
    
    try {
      await jobpostingService.delete(jobpostingId)
      // 목록에서 제거
      jobpostings.value = jobpostings.value.filter(j => j.jobpostingId !== jobpostingId)
      myJobpostings.value = myJobpostings.value.filter(j => j.jobpostingId !== jobpostingId)
      // 현재 상세 초기화
      if (currentJobposting.value?.jobpostingId === jobpostingId) {
        currentJobposting.value = null
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : '채용공고 삭제에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // ============================================
  // Actions - Like
  // ============================================
  
  // 좋아요 토글
  async function toggleLike(jobpostingId: number): Promise<void> {
    if (!currentJobposting.value) return
    
    try {
      if (currentJobposting.value.isLiked) {
        await likeService.unlike(jobpostingId)
        currentJobposting.value.isLiked = false
        currentJobposting.value.likeCount--
      } else {
        await likeService.like(jobpostingId)
        currentJobposting.value.isLiked = true
        currentJobposting.value.likeCount++
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : '좋아요 처리에 실패했습니다.'
      throw e
    }
  }

  // ============================================
  // Actions - Comment
  // ============================================
  
  // 댓글 목록 조회
  async function fetchComments(jobpostingId: number, page?: number): Promise<void> {
    loading.value = true
    error.value = null
    
    try {
      if (page !== undefined) commentPage.value = page
      
      const response = await commentService.getList(
        jobpostingId,
        commentPage.value,
        commentPageSize.value
      )
      
      comments.value = response.comments
      commentTotalCount.value = response.commentCount
    } catch (e) {
      error.value = e instanceof Error ? e.message : '댓글을 불러오는데 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 댓글 작성
  async function createComment(request: CommentCreateRequest): Promise<Comment> {
    loading.value = true
    error.value = null
    
    try {
      const comment = await commentService.create(request)
      // 댓글 목록 갱신
      await fetchComments(request.jobpostingId, 1)
      // 댓글 수 증가
      if (currentJobposting.value?.jobpostingId === request.jobpostingId) {
        currentJobposting.value.commentCount++
      }
      return comment
    } catch (e) {
      error.value = e instanceof Error ? e.message : '댓글 작성에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 댓글 삭제
  async function deleteComment(commentId: number, jobpostingId: number): Promise<void> {
    loading.value = true
    error.value = null
    
    try {
      await commentService.delete(commentId)
      // 댓글 목록에서 제거
      comments.value = comments.value.filter(c => c.commentId !== commentId)
      // 댓글 수 감소
      if (currentJobposting.value?.jobpostingId === jobpostingId) {
        currentJobposting.value.commentCount--
      }
      commentTotalCount.value--
    } catch (e) {
      error.value = e instanceof Error ? e.message : '댓글 삭제에 실패했습니다.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // ============================================
  // Utils
  // ============================================
  
  function clearError(): void {
    error.value = null
  }

  function clearCurrentJobposting(): void {
    currentJobposting.value = null
    comments.value = []
  }

  function setPage(page: number): void {
    currentPage.value = page
  }

  function setBoardId(boardId: number): void {
    currentBoardId.value = boardId
    currentPage.value = 1  // 게시판 변경 시 첫 페이지로
  }

  return {
    // State
    jobpostings,
    currentJobposting,
    myJobpostings,
    comments,
    loading,
    error,
    currentPage,
    pageSize,
    totalCount,
    currentBoardId,
    commentPage,
    commentPageSize,
    commentTotalCount,
    
    // Getters
    totalPages,
    hasNextPage,
    hasPrevPage,
    commentTotalPages,
    
    // Actions - Jobposting
    fetchJobpostings,
    fetchJobposting,
    fetchMyJobpostings,
    createJobposting,
    updateJobposting,
    deleteJobposting,
    
    // Actions - Like
    toggleLike,
    
    // Actions - Comment
    fetchComments,
    createComment,
    deleteComment,
    
    // Utils
    clearError,
    clearCurrentJobposting,
    setPage,
    setBoardId,
  }
})
