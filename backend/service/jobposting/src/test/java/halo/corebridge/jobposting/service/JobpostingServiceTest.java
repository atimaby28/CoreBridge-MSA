package halo.corebridge.jobposting.service;

import halo.corebridge.common.outboxmessagerelay.OutboxEventPublisher;
import halo.corebridge.jobposting.client.AiServiceClient;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import halo.corebridge.jobposting.model.entity.Jobposting;
import halo.corebridge.jobposting.repository.JobpostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobpostingService 테스트")
class JobpostingServiceTest {

    @InjectMocks
    private JobpostingService jobpostingService;

    @Mock
    private JobpostingRepository jobpostingRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private static final Long USER_ID = 100L;
    private Jobposting testJobposting;

    @BeforeEach
    void setUp() {
        testJobposting = Jobposting.create(1L, "백엔드 개발자 채용", "Java/Spring 경력자",
                1L, USER_ID, "[\"Java\",\"Spring\"]", "[\"Kafka\"]");
    }

    @Nested
    @DisplayName("채용공고 생성")
    class CreateTests {

        @Test
        @DisplayName("성공: 공고 생성 후 응답 반환")
        void create_success() {
            // given
            JobpostingDto.JobpostingCreateRequest request = JobpostingDto.JobpostingCreateRequest.builder()
                    .title("백엔드 개발자")
                    .content("Java/Spring 경력자 모집")
                    .boardId(1L)
                    .requiredSkills(List.of("Java", "Spring"))
                    .preferredSkills(List.of("Kafka"))
                    .build();

            given(jobpostingRepository.save(any(Jobposting.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            JobpostingDto.JobpostingResponse result = jobpostingService.create(USER_ID, request);

            // then
            assertThat(result.getTitle()).isEqualTo("백엔드 개발자");
            verify(jobpostingRepository).save(any(Jobposting.class));
            verify(outboxEventPublisher).publish(any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("채용공고 조회")
    class ReadTests {

        @Test
        @DisplayName("성공: 단건 조회")
        void read_success() {
            given(jobpostingRepository.findById(1L)).willReturn(Optional.of(testJobposting));

            JobpostingDto.JobpostingResponse result = jobpostingService.read(1L);

            assertThat(result.getTitle()).isEqualTo("백엔드 개발자 채용");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고 조회")
        void read_notFound() {
            given(jobpostingRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> jobpostingService.read(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채용공고를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("성공: 작성자별 목록 조회")
        void readByWriter_success() {
            given(jobpostingRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .willReturn(List.of(testJobposting));

            JobpostingDto.JobpostingListResponse result = jobpostingService.readByWriter(USER_ID);

            assertThat(result.getJobpostings()).hasSize(1);
        }

        @Test
        @DisplayName("성공: 게시판별 페이징 조회")
        void readAll_success() {
            given(jobpostingRepository.findAll(anyLong(), anyLong(), anyLong()))
                    .willReturn(List.of(testJobposting));
            given(jobpostingRepository.count(anyLong(), anyLong())).willReturn(1L);

            JobpostingDto.JobpostingPageResponse result = jobpostingService.readAll(2L, 1L, 10L);

            assertThat(result.getJobpostings()).hasSize(1);
            assertThat(result.getJobpostingCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("채용공고 수정")
    class UpdateTests {

        @Test
        @DisplayName("성공: 본인 공고 수정")
        void update_success() {
            given(jobpostingRepository.findById(1L)).willReturn(Optional.of(testJobposting));

            JobpostingDto.JobpostingUpdateRequest request = JobpostingDto.JobpostingUpdateRequest.builder()
                    .title("수정된 제목")
                    .content("수정된 내용")
                    .build();

            JobpostingDto.JobpostingResponse result = jobpostingService.update(USER_ID, 1L, request);

            assertThat(result.getTitle()).isEqualTo("수정된 제목");
            verify(outboxEventPublisher).publish(any(), any(), anyLong());
        }

        @Test
        @DisplayName("실패: 타인 공고 수정 시 예외")
        void update_notOwner_throws() {
            given(jobpostingRepository.findById(1L)).willReturn(Optional.of(testJobposting));

            JobpostingDto.JobpostingUpdateRequest request = JobpostingDto.JobpostingUpdateRequest.builder()
                    .title("수정").content("내용").build();

            assertThatThrownBy(() -> jobpostingService.update(999L, 1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("본인이 작성한");
        }
    }

    @Nested
    @DisplayName("채용공고 삭제")
    class DeleteTests {

        @Test
        @DisplayName("성공: 본인 공고 삭제")
        void delete_success() {
            given(jobpostingRepository.findById(1L)).willReturn(Optional.of(testJobposting));

            jobpostingService.delete(USER_ID, 1L);

            verify(jobpostingRepository).deleteById(1L);
            verify(outboxEventPublisher).publish(any(), any(), anyLong());
        }

        @Test
        @DisplayName("실패: 타인 공고 삭제 시 예외")
        void delete_notOwner_throws() {
            given(jobpostingRepository.findById(1L)).willReturn(Optional.of(testJobposting));

            assertThatThrownBy(() -> jobpostingService.delete(999L, 1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 공고 삭제 시 예외")
        void delete_notFound_throws() {
            given(jobpostingRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> jobpostingService.delete(USER_ID, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
