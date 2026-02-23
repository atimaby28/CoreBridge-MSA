package halo.corebridge.jobpostinglike.service;

import halo.corebridge.jobpostinglike.dto.JobpostingLikeResponse;
import halo.corebridge.jobpostinglike.entity.JobpostingLike;
import halo.corebridge.jobpostinglike.entity.JobpostingLikeCount;
import halo.corebridge.jobpostinglike.repository.JobpostingLikeCountRepository;
import halo.corebridge.jobpostinglike.repository.JobpostingLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobpostingLikeServiceTest {

    @InjectMocks
    JobpostingLikeService jobpostingLikeService;

    @Mock
    JobpostingLikeRepository jobpostingLikeRepository;

    @Mock
    JobpostingLikeCountRepository jobpostingLikeCountRepository;

    @Test
    @DisplayName("좋아요 상태 조회 - 좋아요한 경우")
    void read_liked_shouldReturnLikedResponse() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;
        JobpostingLike like = JobpostingLike.create(1L, jobpostingId, userId);

        given(jobpostingLikeRepository.findByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(Optional.of(like));

        // when
        JobpostingLikeResponse result = jobpostingLikeService.read(jobpostingId, userId);

        // then
        assertThat(result.isLiked()).isTrue();
        assertThat(result.getJobpostingId()).isEqualTo(jobpostingId);
    }

    @Test
    @DisplayName("좋아요 상태 조회 - 좋아요 안 한 경우")
    void read_notLiked_shouldReturnNotLikedResponse() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingLikeRepository.findByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(Optional.empty());

        // when
        JobpostingLikeResponse result = jobpostingLikeService.read(jobpostingId, userId);

        // then
        assertThat(result.isLiked()).isFalse();
    }

    @Test
    @DisplayName("좋아요 수 조회 - 레코드 있으면 카운트 반환")
    void count_existingRecord_shouldReturnCount() {
        // given
        Long jobpostingId = 1L;
        given(jobpostingLikeCountRepository.findById(jobpostingId))
                .willReturn(Optional.of(JobpostingLikeCount.init(jobpostingId, 10L)));

        // when
        Long result = jobpostingLikeService.count(jobpostingId);

        // then
        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("좋아요 수 조회 - 레코드 없으면 0 반환")
    void count_noRecord_shouldReturnZero() {
        // given
        Long jobpostingId = 1L;
        given(jobpostingLikeCountRepository.findById(jobpostingId))
                .willReturn(Optional.empty());

        // when
        Long result = jobpostingLikeService.count(jobpostingId);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("좋아요 - 처음 좋아요하면 Like 저장 + Count 증가")
    void like_firstTime_shouldSaveAndIncrease() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingLikeRepository.existsByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(false);
        given(jobpostingLikeCountRepository.increase(jobpostingId)).willReturn(1);

        // when
        jobpostingLikeService.like(jobpostingId, userId);

        // then
        verify(jobpostingLikeRepository).save(any(JobpostingLike.class));
        verify(jobpostingLikeCountRepository).increase(jobpostingId);
    }

    @Test
    @DisplayName("좋아요 - 이미 좋아요한 경우 무시")
    void like_alreadyLiked_shouldIgnore() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingLikeRepository.existsByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(true);

        // when
        jobpostingLikeService.like(jobpostingId, userId);

        // then
        verify(jobpostingLikeRepository, never()).save(any());
        verify(jobpostingLikeCountRepository, never()).increase(any());
    }

    @Test
    @DisplayName("좋아요 - 첫 좋아요면 Count 레코드 생성")
    void like_firstLikeOnPost_shouldCreateCountRecord() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingLikeRepository.existsByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(false);
        given(jobpostingLikeCountRepository.increase(jobpostingId)).willReturn(0);  // UPDATE 실패

        // when
        jobpostingLikeService.like(jobpostingId, userId);

        // then
        verify(jobpostingLikeCountRepository).save(any(JobpostingLikeCount.class));
    }

    @Test
    @DisplayName("좋아요 취소 - 좋아요한 상태면 삭제 + Count 감소")
    void unlike_liked_shouldDeleteAndDecrease() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;
        JobpostingLike like = JobpostingLike.create(1L, jobpostingId, userId);

        given(jobpostingLikeRepository.findByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(Optional.of(like));

        // when
        jobpostingLikeService.unlike(jobpostingId, userId);

        // then
        verify(jobpostingLikeRepository).delete(like);
        verify(jobpostingLikeCountRepository).decrease(jobpostingId);
    }

    @Test
    @DisplayName("좋아요 취소 - 좋아요 안 한 상태면 무시")
    void unlike_notLiked_shouldIgnore() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingLikeRepository.findByJobpostingIdAndUserId(jobpostingId, userId))
                .willReturn(Optional.empty());

        // when
        jobpostingLikeService.unlike(jobpostingId, userId);

        // then
        verify(jobpostingLikeRepository, never()).delete(any());
        verify(jobpostingLikeCountRepository, never()).decrease(any());
    }
}
