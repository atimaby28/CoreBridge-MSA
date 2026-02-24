package halo.corebridge.jobpostingcomment.service;

import halo.corebridge.common.outboxmessagerelay.OutboxEventPublisher;
import halo.corebridge.jobpostingcomment.model.dto.CommentDto;
import halo.corebridge.jobpostingcomment.model.entity.Comment;
import halo.corebridge.jobpostingcomment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @InjectMocks
    CommentService commentService;

    @Mock
    CommentRepository commentRepository;

    @Mock
    OutboxEventPublisher outboxEventPublisher;

    private static final Long USER_ID = 100L;
    private static final Long JOBPOSTING_ID = 1L;

    @Nested
    @DisplayName("댓글 조회")
    class ReadTests {

        @Test
        @DisplayName("성공: 댓글 단건 조회")
        void read_success() {
            Comment comment = mock(Comment.class);
            given(comment.getCommentId()).willReturn(10L);
            given(comment.getContent()).willReturn("테스트 댓글");
            given(comment.getJobpostingId()).willReturn(JOBPOSTING_ID);
            given(comment.getUserId()).willReturn(USER_ID);
            given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

            CommentDto.CommentResponse result = commentService.read(10L);

            assertThat(result.getContent()).isEqualTo("테스트 댓글");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 댓글 조회")
        void read_notFound() {
            given(commentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.read(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("댓글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteTests {

        @Test
        @DisplayName("자식 있으면 삭제 표시만 한다")
        void delete_hasChildren_markDeleted() {
            Long commentId = 2L;
            Comment comment = createComment(JOBPOSTING_ID, commentId, USER_ID);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.countBy(JOBPOSTING_ID, commentId, 2L)).willReturn(2L);

            commentService.delete(commentId, USER_ID);

            verify(comment).delete();
        }

        @Test
        @DisplayName("하위 댓글 삭제 시, 부모가 삭제 안 됐으면 자식만 삭제")
        void delete_childOnly_ifParentNotDeleted() {
            Long commentId = 2L;
            Long parentId = 1L;

            Comment comment = createComment(JOBPOSTING_ID, commentId, parentId, USER_ID);
            given(comment.isRoot()).willReturn(false);

            Comment parent = mock(Comment.class);
            given(parent.getDeleted()).willReturn(false);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.countBy(JOBPOSTING_ID, commentId, 2L)).willReturn(1L);
            given(commentRepository.findById(parentId)).willReturn(Optional.of(parent));

            commentService.delete(commentId, USER_ID);

            verify(commentRepository).delete(comment);
            verify(commentRepository, never()).delete(parent);
        }

        @Test
        @DisplayName("부모도 삭제 상태면 재귀적으로 모두 삭제")
        void delete_recursively_ifParentDeleted() {
            Long commentId = 2L;
            Long parentId = 1L;

            Comment comment = createComment(JOBPOSTING_ID, commentId, parentId, USER_ID);
            given(comment.isRoot()).willReturn(false);

            Comment parent = createComment(JOBPOSTING_ID, parentId, USER_ID);
            given(parent.isRoot()).willReturn(true);
            given(parent.getDeleted()).willReturn(true);

            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.countBy(JOBPOSTING_ID, commentId, 2L)).willReturn(1L);
            given(commentRepository.findById(parentId)).willReturn(Optional.of(parent));
            given(commentRepository.countBy(JOBPOSTING_ID, parentId, 2L)).willReturn(1L);

            commentService.delete(commentId, USER_ID);

            verify(commentRepository).delete(comment);
            verify(commentRepository).delete(parent);
        }

        @Test
        @DisplayName("실패: 타인의 댓글 삭제 시 예외")
        void delete_notOwner_throws() {
            Long commentId = 2L;
            Comment comment = createComment(JOBPOSTING_ID, commentId, USER_ID);
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.delete(commentId, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인의 댓글만");
        }
    }

    // ============================================
    // 헬퍼
    // ============================================

    private Comment createComment(Long jobpostingId, Long commentId, Long userId) {
        Comment comment = mock(Comment.class);
        lenient().when(comment.getJobpostingId()).thenReturn(jobpostingId);
        lenient().when(comment.getCommentId()).thenReturn(commentId);
        lenient().when(comment.getUserId()).thenReturn(userId);
        lenient().when(comment.getDeleted()).thenReturn(false);
        return comment;
    }

    private Comment createComment(Long jobpostingId, Long commentId, Long parentId, Long userId) {
        Comment comment = createComment(jobpostingId, commentId, userId);
        lenient().when(comment.getParentCommentId()).thenReturn(parentId);
        return comment;
    }
}
