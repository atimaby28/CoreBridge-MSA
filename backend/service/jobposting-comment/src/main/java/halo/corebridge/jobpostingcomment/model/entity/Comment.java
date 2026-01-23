package halo.corebridge.jobpostingcomment.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {
    @Id
    private Long commentId;
    private String content;
    private Long parentCommentId;
    private Long jobpostingId;
    private Long userId;
    private Boolean deleted;

    public static Comment create(Long commentId, String content, Long parentCommentId, Long jobpostingId, Long userId) {
        Comment comment = new Comment();
        comment.commentId = commentId;
        comment.content = content;
        comment.parentCommentId = parentCommentId == null ? commentId : parentCommentId;
        comment.jobpostingId = jobpostingId;
        comment.userId = userId;
        comment.deleted = false;

        return comment;
    }

    public boolean isRoot() {
        return parentCommentId.longValue() == commentId;
    }

    public void delete() {
        deleted = true;
    }
}
