package halo.corebridge.apply.service;

import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ApplyRepository;
import halo.corebridge.apply.repository.RecruitmentProcessRepository;
import halo.corebridge.common.exception.BaseException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplyServiceTest {

    @InjectMocks
    private ApplyService applyService;

    @Mock
    private ApplyRepository applyRepository;

    @Mock
    private RecruitmentProcessRepository processRepository;

    @Mock
    private ProcessService processService;

    private Apply testApply;
    private RecruitmentProcess testProcess;

    @BeforeEach
    void setUp() {
        testApply = Apply.create(1L, 100L, 200L, 300L, "자기소개서입니다.");
        testProcess = RecruitmentProcess.create(10L, 1L, 100L, 200L);
    }

    @Nested
    @DisplayName("지원하기")
    class ApplyTests {

        @Test
        @DisplayName("성공: 새로운 지원 생성")
        void apply_success() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .coverLetter("열심히 하겠습니다.")
                    .build();

            given(applyRepository.existsByJobpostingIdAndUserId(100L, 200L)).willReturn(false);
            given(applyRepository.save(any(Apply.class))).willAnswer(invocation -> {
                Apply saved = invocation.getArgument(0);
                return saved;
            });
            given(processService.createProcess(any(), eq(100L), eq(200L))).willReturn(testProcess);

            // when
            ApplyDto.ApplyDetailResponse result = applyService.apply(request);

            // then
            assertThat(result.getJobpostingId()).isEqualTo(100L);
            assertThat(result.getUserId()).isEqualTo(200L);
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.APPLIED);
            verify(applyRepository, times(1)).save(any(Apply.class));
            verify(processService, times(1)).createProcess(any(), eq(100L), eq(200L));
        }

        @Test
        @DisplayName("실패: 중복 지원")
        void apply_duplicate_throwsException() {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .build();

            given(applyRepository.existsByJobpostingIdAndUserId(100L, 200L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> applyService.apply(request))
                    .isInstanceOf(BaseException.class);

            verify(applyRepository, never()).save(any());
            verify(processService, never()).createProcess(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("지원 취소")
    class CancelTests {

        @Test
        @DisplayName("성공: APPLIED 상태에서 취소")
        void cancel_success() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            applyService.cancel(1L, 200L);

            // then
            verify(processRepository, times(1)).delete(testProcess);
            verify(applyRepository, times(1)).delete(testApply);
        }

        @Test
        @DisplayName("실패: 본인이 아닌 경우")
        void cancel_notOwner_throwsException() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));

            // when & then
            assertThatThrownBy(() -> applyService.cancel(1L, 999L))
                    .isInstanceOf(BaseException.class);

            verify(processRepository, never()).delete(any());
            verify(applyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 진행 중인 상태에서 취소 시도")
        void cancel_inProgress_throwsException() {
            // given
            testProcess.transition(ProcessStep.DOCUMENT_REVIEW);
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when & then
            assertThatThrownBy(() -> applyService.cancel(1L, 200L))
                    .isInstanceOf(BaseException.class);

            verify(processRepository, never()).delete(any());
            verify(applyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 지원")
        void cancel_notFound_throwsException() {
            // given
            given(applyRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> applyService.cancel(999L, 200L))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("지원 조회")
    class ReadTests {

        @Test
        @DisplayName("성공: 지원 상세 조회")
        void read_success() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            ApplyDto.ApplyDetailResponse result = applyService.read(1L);

            // then
            assertThat(result.getApplyId()).isEqualTo(1L);
            assertThat(result.getJobpostingId()).isEqualTo(100L);
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.APPLIED);
        }

        @Test
        @DisplayName("성공: 내 지원 목록 조회")
        void getMyApplies_success() {
            // given
            List<Apply> applies = List.of(testApply);
            given(applyRepository.findByUserIdOrderByCreatedAtDesc(200L)).willReturn(applies);
            given(applyRepository.countByUserId(200L)).willReturn(1L);
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            ApplyDto.ApplyPageResponse result = applyService.getMyApplies(200L);

            // then
            assertThat(result.getApplies()).hasSize(1);
            assertThat(result.getTotalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공: 공고별 지원자 목록 조회")
        void getAppliesByJobposting_success() {
            // given
            List<Apply> applies = List.of(testApply);
            given(applyRepository.findByJobpostingIdOrderByCreatedAtDesc(100L)).willReturn(applies);
            given(applyRepository.countByJobpostingId(100L)).willReturn(1L);
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            // when
            ApplyDto.ApplyPageResponse result = applyService.getAppliesByJobposting(100L);

            // then
            assertThat(result.getApplies()).hasSize(1);
            assertThat(result.getTotalCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공: 공고별 특정 단계 지원자 목록 조회")
        void getAppliesByStep_success() {
            // given
            List<RecruitmentProcess> processes = List.of(testProcess);
            given(processRepository.findByJobpostingIdAndCurrentStepOrderByStepChangedAtDesc(100L, ProcessStep.APPLIED))
                    .willReturn(processes);
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));

            // when
            ApplyDto.ApplyPageResponse result = applyService.getAppliesByStep(100L, ProcessStep.APPLIED);

            // then
            assertThat(result.getApplies()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("메모 수정")
    class UpdateMemoTests {

        @Test
        @DisplayName("성공: 메모 수정")
        void updateMemo_success() {
            // given
            given(applyRepository.findById(1L)).willReturn(Optional.of(testApply));
            given(processRepository.findByApplyId(1L)).willReturn(Optional.of(testProcess));

            ApplyDto.UpdateMemoRequest request = ApplyDto.UpdateMemoRequest.builder()
                    .memo("면접 시 추가 질문 필요")
                    .build();

            // when
            ApplyDto.ApplyDetailResponse result = applyService.updateMemo(1L, request);

            // then
            assertThat(result.getMemo()).isEqualTo("면접 시 추가 질문 필요");
        }
    }
}
