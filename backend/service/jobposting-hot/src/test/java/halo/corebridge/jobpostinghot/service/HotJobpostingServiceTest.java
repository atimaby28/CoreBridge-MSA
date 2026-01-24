package halo.corebridge.jobpostinghot.service;

import halo.corebridge.jobpostinghot.client.CommentClient;
import halo.corebridge.jobpostinghot.client.JobpostingClient;
import halo.corebridge.jobpostinghot.client.LikeClient;
import halo.corebridge.jobpostinghot.client.ViewClient;
import halo.corebridge.jobpostinghot.model.dto.HotJobpostingDto;
import halo.corebridge.jobpostinghot.model.entity.HotJobposting;
import halo.corebridge.jobpostinghot.repository.HotJobpostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HotJobpostingServiceTest {

    @Mock
    private HotJobpostingRepository hotJobpostingRepository;
    @Mock
    private JobpostingClient jobpostingClient;
    @Mock
    private ViewClient viewClient;
    @Mock
    private LikeClient likeClient;
    @Mock
    private CommentClient commentClient;

    @InjectMocks
    private HotJobpostingService hotJobpostingService;

    private HotJobposting mockHotJobposting;
    private JobpostingClient.JobpostingResponse mockJobposting;

    @BeforeEach
    void setUp() {
        mockHotJobposting = HotJobposting.create(
                LocalDate.now(),
                1L,
                "테스트 채용공고",
                1L,
                10L,
                5L,
                100L
        );

        mockJobposting = new JobpostingClient.JobpostingResponse();
        mockJobposting.setJobpostingId(1L);
        mockJobposting.setTitle("테스트 채용공고");
        mockJobposting.setBoardId(1L);
    }

    @Test
    @DisplayName("오늘의 인기 공고 조회 - 성공")
    void readTopN_success() {
        // given
        given(hotJobpostingRepository.findTopByDateKey(LocalDate.now(), 10))
                .willReturn(List.of(mockHotJobposting));

        // when
        List<HotJobpostingDto.Response> result = hotJobpostingService.readTopN(10);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobpostingId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 채용공고");
    }

    @Test
    @DisplayName("특정 날짜 인기 공고 조회 - 성공")
    void readAll_success() {
        // given
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        given(hotJobpostingRepository.findByDateKeyOrderByScoreDesc(LocalDate.now()))
                .willReturn(List.of(mockHotJobposting));

        // when
        List<HotJobpostingDto.Response> result = hotJobpostingService.readAll(dateStr);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(10 * 3.0 + 5 * 2.0 + 100 * 1.0);
    }

    @Test
    @DisplayName("인기 공고 등록 - 성공")
    void register_success() {
        // given
        Long jobpostingId = 1L;
        given(jobpostingClient.read(jobpostingId)).willReturn(mockJobposting);
        given(viewClient.count(jobpostingId)).willReturn(100L);
        given(likeClient.count(jobpostingId)).willReturn(10L);
        given(commentClient.count(jobpostingId)).willReturn(5L);
        given(hotJobpostingRepository.findById(any())).willReturn(Optional.empty());
        given(hotJobpostingRepository.save(any())).willReturn(mockHotJobposting);

        // when
        HotJobpostingDto.Response result = hotJobpostingService.register(jobpostingId);

        // then
        assertThat(result).isNotNull();
        verify(hotJobpostingRepository).save(any(HotJobposting.class));
    }

    @Test
    @DisplayName("인기 공고 등록 - 존재하지 않는 채용공고")
    void register_notFound() {
        // given
        Long jobpostingId = 999L;
        given(jobpostingClient.read(jobpostingId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> hotJobpostingService.register(jobpostingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Jobposting not found");
    }
}
