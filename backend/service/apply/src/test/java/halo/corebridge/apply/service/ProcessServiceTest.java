package halo.corebridge.apply.service;

import halo.corebridge.apply.model.dto.ProcessDto;
import halo.corebridge.apply.model.entity.ProcessHistory;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ProcessHistoryRepository;
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
class ProcessServiceTest {

    @InjectMocks
    private ProcessService processService;

    @Mock
    private RecruitmentProcessRepository processRepository;

    @Mock
    private ProcessHistoryRepository historyRepository;

    private RecruitmentProcess testProcess;

    @BeforeEach
    void setUp() {
        testProcess = RecruitmentProcess.create(1L, 100L, 200L, 300L);
    }

    @Nested
    @DisplayName("프로세스 생성")
    class CreateProcess {

        @Test
        @DisplayName("성공: 프로세스 생성 시 APPLIED 상태로 시작")
        void createProcess_success() {
            // given
            given(processRepository.save(any(RecruitmentProcess.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(historyRepository.save(any(ProcessHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            RecruitmentProcess result = processService.createProcess(100L, 200L, 300L);

            // then
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.APPLIED);
            assertThat(result.getApplyId()).isEqualTo(100L);
            assertThat(result.getJobpostingId()).isEqualTo(200L);
            assertThat(result.getUserId()).isEqualTo(300L);
            verify(processRepository, times(1)).save(any(RecruitmentProcess.class));
            verify(historyRepository, times(1)).save(any(ProcessHistory.class));
        }
    }

    @Nested
    @DisplayName("상태 전이 (State Machine)")
    class Transition {

        @Test
        @DisplayName("성공: APPLIED → DOCUMENT_REVIEW 전이")
        void transition_appliedToDocumentReview_success() {
            // given
            given(processRepository.findById(1L)).willReturn(Optional.of(testProcess));
            given(historyRepository.save(any(ProcessHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.DOCUMENT_REVIEW)
                    .changedBy(500L)
                    .reason("서류 검토 시작")
                    .build();

            // when
            ProcessDto.ProcessResponse result = processService.transition(1L, request);

            // then
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.DOCUMENT_REVIEW);
            assertThat(result.getPreviousStep()).isEqualTo(ProcessStep.APPLIED);
            verify(historyRepository, times(1)).save(any(ProcessHistory.class));
        }

        @Test
        @DisplayName("성공: DOCUMENT_REVIEW → DOCUMENT_PASS 전이")
        void transition_documentReviewToPass_success() {
            // given
            testProcess.transition(ProcessStep.DOCUMENT_REVIEW);
            given(processRepository.findById(1L)).willReturn(Optional.of(testProcess));
            given(historyRepository.save(any(ProcessHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.DOCUMENT_PASS)
                    .changedBy(500L)
                    .reason("서류 합격")
                    .build();

            // when
            ProcessDto.ProcessResponse result = processService.transition(1L, request);

            // then
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.DOCUMENT_PASS);
        }

        @Test
        @DisplayName("성공: DOCUMENT_REVIEW → DOCUMENT_FAIL 전이")
        void transition_documentReviewToFail_success() {
            // given
            testProcess.transition(ProcessStep.DOCUMENT_REVIEW);
            given(processRepository.findById(1L)).willReturn(Optional.of(testProcess));
            given(historyRepository.save(any(ProcessHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.DOCUMENT_FAIL)
                    .changedBy(500L)
                    .reason("서류 불합격")
                    .build();

            // when
            ProcessDto.ProcessResponse result = processService.transition(1L, request);

            // then
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.DOCUMENT_FAIL);
            assertThat(result.isFailed()).isTrue();
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("실패: APPLIED → FINAL_PASS 직접 전이 불가")
        void transition_invalidTransition_throwsException() {
            // given
            given(processRepository.findById(1L)).willReturn(Optional.of(testProcess));

            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.FINAL_PASS)
                    .changedBy(500L)
                    .reason("바로 합격?")
                    .build();

            // when & then
            assertThatThrownBy(() -> processService.transition(1L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("전이할 수 없습니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 프로세스")
        void transition_processNotFound_throwsException() {
            // given
            given(processRepository.findById(999L)).willReturn(Optional.empty());

            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.DOCUMENT_REVIEW)
                    .build();

            // when & then
            assertThatThrownBy(() -> processService.transition(999L, request))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("전체 채용 프로세스 시나리오")
    class FullProcessScenario {

        @Test
        @DisplayName("성공: 전체 합격 시나리오 (APPLIED → FINAL_PASS)")
        void fullPassScenario() {
            // given
            RecruitmentProcess process = RecruitmentProcess.create(1L, 100L, 200L, 300L);

            // when: 전체 채용 프로세스 진행
            process.transition(ProcessStep.DOCUMENT_REVIEW);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.DOCUMENT_REVIEW);

            process.transition(ProcessStep.DOCUMENT_PASS);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.DOCUMENT_PASS);

            process.transition(ProcessStep.CODING_TEST);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.CODING_TEST);

            process.transition(ProcessStep.CODING_PASS);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.CODING_PASS);

            process.transition(ProcessStep.INTERVIEW_1);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.INTERVIEW_1);

            process.transition(ProcessStep.INTERVIEW_1_PASS);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.INTERVIEW_1_PASS);

            process.transition(ProcessStep.INTERVIEW_2);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.INTERVIEW_2);

            process.transition(ProcessStep.INTERVIEW_2_PASS);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.INTERVIEW_2_PASS);

            process.transition(ProcessStep.FINAL_REVIEW);
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.FINAL_REVIEW);

            process.transition(ProcessStep.FINAL_PASS);

            // then
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.FINAL_PASS);
            assertThat(process.isCompleted()).isTrue();
            assertThat(process.isPassed()).isTrue();
            assertThat(process.isFailed()).isFalse();
        }

        @Test
        @DisplayName("성공: 서류 탈락 시나리오")
        void documentFailScenario() {
            // given
            RecruitmentProcess process = RecruitmentProcess.create(1L, 100L, 200L, 300L);

            // when
            process.transition(ProcessStep.DOCUMENT_REVIEW);
            process.transition(ProcessStep.DOCUMENT_FAIL);

            // then
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.DOCUMENT_FAIL);
            assertThat(process.isCompleted()).isTrue();
            assertThat(process.isFailed()).isTrue();
            assertThat(process.isPassed()).isFalse();
        }

        @Test
        @DisplayName("성공: 1차 면접 탈락 시나리오")
        void interview1FailScenario() {
            // given
            RecruitmentProcess process = RecruitmentProcess.create(1L, 100L, 200L, 300L);

            // when
            process.transition(ProcessStep.DOCUMENT_REVIEW);
            process.transition(ProcessStep.DOCUMENT_PASS);
            process.transition(ProcessStep.INTERVIEW_1);
            process.transition(ProcessStep.INTERVIEW_1_FAIL);

            // then
            assertThat(process.getCurrentStep()).isEqualTo(ProcessStep.INTERVIEW_1_FAIL);
            assertThat(process.isCompleted()).isTrue();
            assertThat(process.isFailed()).isTrue();
        }
    }

    @Nested
    @DisplayName("조회")
    class Read {

        @Test
        @DisplayName("성공: 프로세스 상세 조회")
        void read_success() {
            // given
            given(processRepository.findById(1L)).willReturn(Optional.of(testProcess));

            // when
            ProcessDto.ProcessResponse result = processService.read(1L);

            // then
            assertThat(result.getProcessId()).isEqualTo(1L);
            assertThat(result.getCurrentStep()).isEqualTo(ProcessStep.APPLIED);
        }

        @Test
        @DisplayName("성공: 지원 ID로 프로세스 조회")
        void readByApplyId_success() {
            // given
            given(processRepository.findByApplyId(100L)).willReturn(Optional.of(testProcess));

            // when
            ProcessDto.ProcessResponse result = processService.readByApplyId(100L);

            // then
            assertThat(result.getApplyId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("통계")
    class Statistics {

        @Test
        @DisplayName("성공: 사용자 통계 조회")
        void getUserStats_success() {
            // given
            Long userId = 300L;
            given(processRepository.countByUserId(userId)).willReturn(10L);
            given(processRepository.countByUserIdAndCurrentStepIn(eq(userId), any()))
                    .willReturn(5L);
            given(processRepository.countByUserIdAndCurrentStep(userId, ProcessStep.FINAL_PASS))
                    .willReturn(2L);

            // when
            ProcessDto.UserStatsResponse result = processService.getUserStats(userId);

            // then
            assertThat(result.getTotalProcesses()).isEqualTo(10L);
        }

        @Test
        @DisplayName("성공: 공고별 통계 조회")
        void getJobpostingStats_success() {
            // given
            Long jobpostingId = 200L;
            given(processRepository.countByJobpostingId(jobpostingId)).willReturn(50L);
            given(processRepository.countByJobpostingIdAndCurrentStepIn(eq(jobpostingId), any()))
                    .willReturn(10L);
            given(processRepository.countByJobpostingIdAndCurrentStep(jobpostingId, ProcessStep.FINAL_PASS))
                    .willReturn(3L);

            // when
            ProcessDto.CompanyStatsResponse result = processService.getJobpostingStats(jobpostingId);

            // then
            assertThat(result.getTotalApplicants()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("메타 정보")
    class MetaInfo {

        @Test
        @DisplayName("성공: 모든 단계 정보 조회")
        void getAllSteps_success() {
            // when
            List<ProcessDto.StepInfoResponse> result = processService.getAllSteps();

            // then
            assertThat(result).hasSize(ProcessStep.values().length);
            assertThat(result.get(0).getStep()).isEqualTo(ProcessStep.APPLIED);
        }
    }
}
