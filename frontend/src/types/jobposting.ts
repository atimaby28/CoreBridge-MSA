// ============================================
// 채용공고 (Jobposting)
// ============================================

// 채용공고 기본 정보
export interface Jobposting {
  jobpostingId: number
  title: string
  content: string
  boardId: number
  userId: number
  createdAt: string
  updatedAt: string
}

// 채용공고 상세 정보 (조회수, 좋아요, 댓글 수 포함)
export interface JobpostingDetail extends Jobposting {
  viewCount: number
  likeCount: number
  commentCount: number
  isLiked?: boolean  // 현재 사용자가 좋아요 했는지
}

// 채용공고 생성 요청
export interface JobpostingCreateRequest {
  title: string
  content: string
  boardId: number
}

// 채용공고 수정 요청
export interface JobpostingUpdateRequest {
  title?: string
  content?: string
}

// 채용공고 목록 응답 (페이징)
export interface JobpostingPageResponse {
  jobpostings: Jobposting[]
  jobpostingCount: number
}

// 채용공고 목록 응답 (리스트)
export interface JobpostingListResponse {
  jobpostings: Jobposting[]
}

// ============================================
// 게시판 (Board)
// ============================================

export interface Board {
  boardId: number
  name: string
  description?: string
}

// 기본 게시판 목록 (하드코딩 - 나중에 API로 변경)
export const DEFAULT_BOARDS: Board[] = [
  { boardId: 1, name: '전체', description: '모든 채용공고' },
  { boardId: 2, name: 'IT/개발', description: 'IT 및 개발 직군' },
  { boardId: 3, name: '마케팅', description: '마케팅 직군' },
  { boardId: 4, name: '디자인', description: '디자인 직군' },
  { boardId: 5, name: '영업', description: '영업 직군' },
]

// ============================================
// 좋아요 (Like)
// ============================================

export interface LikeResponse {
  jobpostingId: number
  userId: number
  liked: boolean
  likeCount: number
}

// ============================================
// 조회수 (View)
// ============================================

export interface ViewCountResponse {
  jobpostingId: number
  viewCount: number
}

// ============================================
// 댓글 (Comment)
// ============================================

export interface Comment {
  commentId: number
  jobpostingId: number
  userId: number
  content: string
  parentCommentId?: number  // 대댓글인 경우
  createdAt: string
  updatedAt: string
  deleted: boolean
}

export interface CommentCreateRequest {
  jobpostingId: number
  content: string
  parentCommentId?: number
}

export interface CommentPageResponse {
  comments: Comment[]
  commentCount: number
}
