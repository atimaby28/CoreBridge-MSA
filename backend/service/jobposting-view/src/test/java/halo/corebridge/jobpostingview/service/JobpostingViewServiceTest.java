package halo.corebridge.jobpostingview.service;

import halo.corebridge.jobpostingview.entity.JobpostingViewCount;
import halo.corebridge.jobpostingview.repository.JobpostingViewCountRepository;
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
class JobpostingViewServiceTest {

    @InjectMocks
    JobpostingViewService jobpostingViewService;

    @Mock
    JobpostingViewCountRepository jobpostingViewCountRepository;

    @Test
    @DisplayName("조회수 증가 - 기존 레코드 있으면 UPDATE")
    void increase_existingRecord_shouldUpdate() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingViewCountRepository.increase(jobpostingId)).willReturn(1);
        given(jobpostingViewCountRepository.findById(jobpostingId))
                .willReturn(Optional.of(JobpostingViewCount.init(jobpostingId, 5L)));

        // when
        Long result = jobpostingViewService.increase(jobpostingId, userId);

        // then
        assertThat(result).isEqualTo(5L);
        verify(jobpostingViewCountRepository).increase(jobpostingId);
        verify(jobpostingViewCountRepository, never()).save(any());
    }

    @Test
    @DisplayName("조회수 증가 - 최초 조회 시 레코드 생성")
    void increase_firstView_shouldCreateRecord() {
        // given
        Long jobpostingId = 1L;
        Long userId = 100L;

        given(jobpostingViewCountRepository.increase(jobpostingId)).willReturn(0);

        // when
        Long result = jobpostingViewService.increase(jobpostingId, userId);

        // then
        assertThat(result).isEqualTo(1L);
        verify(jobpostingViewCountRepository).save(any(JobpostingViewCount.class));
    }

    @Test
    @DisplayName("조회수 조회 - 레코드 있으면 조회수 반환")
    void count_existingRecord_shouldReturnCount() {
        // given
        Long jobpostingId = 1L;
        given(jobpostingViewCountRepository.findById(jobpostingId))
                .willReturn(Optional.of(JobpostingViewCount.init(jobpostingId, 10L)));

        // when
        Long result = jobpostingViewService.count(jobpostingId);

        // then
        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("조회수 조회 - 레코드 없으면 0 반환")
    void count_noRecord_shouldReturnZero() {
        // given
        Long jobpostingId = 1L;
        given(jobpostingViewCountRepository.findById(jobpostingId))
                .willReturn(Optional.empty());

        // when
        Long result = jobpostingViewService.count(jobpostingId);

        // then
        assertThat(result).isEqualTo(0L);
    }
}
