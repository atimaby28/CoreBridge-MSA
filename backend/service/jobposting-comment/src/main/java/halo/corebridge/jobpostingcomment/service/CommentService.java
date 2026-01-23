package halo.corebridge.jobpostingcomment.service;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.jobpostingcomment.model.dto.CommentDto;
import halo.corebridge.jobpostingcomment.model.entity.Comment;
import halo.corebridge.jobpostingcomment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    @Transactional
    public CommentDto.CommentResponse create(CommentDto.CommentCreateRequest request, Long userId) {
        Comment parent = findParent(request);
        Comment comment = commentRepository.save(
                Comment.create(
                        snowflake.nextId(),
                        request.getContent(),
                        parent == null ? null : parent.getCommentId(),
                        request.getJobpostingId(),
                        userId  // SecurityContext에서 받은 userId 사용
                )
        );
        return CommentDto.CommentResponse.from(comment);
    }

    private Comment findParent(CommentDto.CommentCreateRequest request) {
        Long parentCommentId = request.getParentCommentId();
        if (parentCommentId == null) {
            return null;
        }
        return commentRepository.findById(parentCommentId)
                .filter(not(Comment::getDeleted))
                .filter(Comment::isRoot)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 부모 댓글입니다."));
    }

    public CommentDto.CommentResponse read(Long commentId) {
        return CommentDto.CommentResponse.from(
                commentRepository.findById(commentId)
                        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."))
        );
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        commentRepository.findById(commentId)
                .filter(not(Comment::getDeleted))
                .ifPresent(comment -> {
                    // 본인 댓글만 삭제 가능
                    if (!comment.getUserId().equals(userId)) {
                        throw new IllegalArgumentException("본인의 댓글만 삭제할 수 있습니다.");
                    }

                    if (hasChildren(comment)) {
                        comment.delete();
                    } else {
                        deleteRecursively(comment);
                    }
                });
    }

    private boolean hasChildren(Comment comment) {
        return commentRepository.countBy(comment.getJobpostingId(), comment.getCommentId(), 2L) == 2;
    }

    private void deleteRecursively(Comment comment) {
        commentRepository.delete(comment);
        if (!comment.isRoot()) {
            commentRepository.findById(comment.getParentCommentId())
                    .filter(Comment::getDeleted)
                    .filter(not(this::hasChildren))
                    .ifPresent(this::deleteRecursively);
        }
    }

    public CommentDto.CommentPageResponse readAll(Long jobpostingId, Long page, Long pageSize) {
        return CommentDto.CommentPageResponse.of(
                commentRepository.findAll(jobpostingId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentDto.CommentResponse::from)
                        .toList(),
                commentRepository.count(jobpostingId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L))
        );
    }
}
