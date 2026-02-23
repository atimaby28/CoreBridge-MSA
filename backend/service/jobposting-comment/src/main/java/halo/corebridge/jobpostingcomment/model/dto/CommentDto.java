package halo.corebridge.jobpostingcomment.model.dto;

import halo.corebridge.jobpostingcomment.model.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDto {
    @Builder
    @Getter
    public static class CommentCreateRequest {
        private Long jobpostingId;
        private String content;
        private Long parentCommentId;
        private Long userId;
    }

    @Builder
    @Getter
    public static class CommentResponse {
        private Long commentId;
        private String content;
        private Long parentCommentId;
        private Long jobpostingId;
        private Long userId;
        private Boolean deleted;
        private String path;
        private LocalDateTime createdAt;

        public static CommentResponse from(Comment comment) {
            return CommentResponse.builder()
                    .commentId(comment.getCommentId())
                    .content(comment.getContent())
                    .parentCommentId(comment.getParentCommentId())
                    .jobpostingId(comment.getJobpostingId())
                    .userId(comment.getUserId())
                    .deleted(comment.getDeleted())
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
    }

    @Builder
    @Getter
    public static class CommentPageResponse {
        private List<CommentResponse> comments;
        private Long commentCount;

        public static CommentPageResponse of(List<CommentResponse> comments, Long commentCount) {
            return CommentPageResponse.builder()
                    .comments(comments)
                    .commentCount(commentCount)
                    .build();
        }
    }


}
