package halo.corebridge.jobpostingcomment.service;

import halo.corebridge.jobpostingcomment.model.entity.Comment;
import halo.corebridge.jobpostingcomment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @InjectMocks
    CommentService commentService;
    @Mock
    CommentRepository commentRepository;

    private static final Long USER_ID = 100L;

    @Test
    @DisplayName("삭제할 댓글이 자식 있으면, 삭제 표시만 한다.")
    void deleteShouldMarkDeletedIfHasChildren() {
        // given
        Long jobpostingId = 1L;
        Long commentId = 2L;
        Comment comment = createComment(jobpostingId, commentId, USER_ID);
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentRepository.countBy(jobpostingId, commentId, 2L)).willReturn(2L);

        // when
        commentService.delete(commentId, USER_ID);

        // then
        verify(comment).delete();
    }

    @Test
    @DisplayName("하위 댓글이 삭제되고, 삭제되지 않은 부모면, 하위 댓글만 삭제한다.")
    void deleteShouldDeleteChildOnlyIfNotDeletedParent() {
        // given
        Long jobpostingId = 1L;
        Long commentId = 2L;
        Long parentCommentId = 1L;

        Comment comment = createComment(jobpostingId, commentId, parentCommentId, USER_ID);
        given(comment.isRoot()).willReturn(false);

        Comment parentComment = mock(Comment.class);
        given(parentComment.getDeleted()).willReturn(false);

        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentRepository.countBy(jobpostingId, commentId, 2L)).willReturn(1L);

        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));

        // when
        commentService.delete(commentId, USER_ID);

        // then
        verify(commentRepository).delete(comment);
        verify(commentRepository, never()).delete(parentComment);
    }

    @Test
    @DisplayName("하위 댓글이 삭제되고, 삭제된 부모면, 재귀적으로 모두 삭제한다.")
    void deleteShouldDeleteAllRecursivelyIfDeletedParent() {
        // given
        Long jobpostingId = 1L;
        Long commentId = 2L;
        Long parentCommentId = 1L;

        Comment comment = createComment(jobpostingId, commentId, parentCommentId, USER_ID);
        given(comment.isRoot()).willReturn(false);

        Comment parentComment = createComment(jobpostingId, parentCommentId, USER_ID);
        given(parentComment.isRoot()).willReturn(true);
        given(parentComment.getDeleted()).willReturn(true);

        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        given(commentRepository.countBy(jobpostingId, commentId, 2L)).willReturn(1L);

        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));
        given(commentRepository.countBy(jobpostingId, parentCommentId, 2L)).willReturn(1L);

        // when
        commentService.delete(commentId, USER_ID);

        // then
        verify(commentRepository).delete(comment);
        verify(commentRepository).delete(parentComment);
    }

    private Comment createComment(Long jobpostingId, Long commentId, Long userId) {
        Comment comment = mock(Comment.class);
        given(comment.getJobpostingId()).willReturn(jobpostingId);
        given(comment.getCommentId()).willReturn(commentId);
        given(comment.getUserId()).willReturn(userId);
        given(comment.getDeleted()).willReturn(false);
        return comment;
    }

    private Comment createComment(Long jobpostingId, Long commentId, Long parentCommentId, Long userId) {
        Comment comment = createComment(jobpostingId, commentId, userId);
        given(comment.getParentCommentId()).willReturn(parentCommentId);
        return comment;
    }
}
