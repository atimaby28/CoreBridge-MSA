package halo.corebridge.apply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.apply.config.SecurityConfig;
import halo.corebridge.apply.model.dto.ProcessDto;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.security.JwtAuthenticationFilter;
import halo.corebridge.apply.security.JwtProvider;
import halo.corebridge.apply.service.ProcessService;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProcessController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class,
                        AuditLoggingFilter.class,
                        JwtProvider.class
                }
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ProcessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProcessService processService;

    private ProcessDto.ProcessResponse createTestResponse() {
        return ProcessDto.ProcessResponse.builder()
                .processId(10L)
                .applyId(1L)
                .jobpostingId(100L)
                .userId(200L)
                .currentStep(ProcessStep.APPLIED)
                .currentStepName("지원완료")
                .allowedNextSteps(Set.of("DOCUMENT_REVIEW"))
                .completed(false)
                .passed(false)
                .failed(false)
                .stepChangedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("상태 전이 API (State Machine)")
    class TransitionApiTests {

        @Test
        @DisplayName("성공: 상태 전이")
        void transition_success() throws Exception {
            // given
            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.DOCUMENT_REVIEW)
                    .changedBy(500L)
                    .reason("서류 검토 시작")
                    .build();

            ProcessDto.ProcessResponse response = ProcessDto.ProcessResponse.builder()
                    .processId(10L)
                    .currentStep(ProcessStep.DOCUMENT_REVIEW)
                    .currentStepName("서류검토중")
                    .previousStep(ProcessStep.APPLIED)
                    .previousStepName("지원완료")
                    .completed(false)
                    .build();

            given(processService.transition(eq(10L), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/processes/10/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.currentStep").value("DOCUMENT_REVIEW"))
                    .andExpect(jsonPath("$.result.previousStep").value("APPLIED"));
        }

        @Test
        @DisplayName("성공: 지원 ID로 상태 전이")
        void transitionByApply_success() throws Exception {
            // given
            ProcessDto.TransitionRequest request = ProcessDto.TransitionRequest.builder()
                    .nextStep(ProcessStep.DOCUMENT_REVIEW)
                    .changedBy(500L)
                    .reason("서류 검토 시작")
                    .build();

            ProcessDto.ProcessResponse response = ProcessDto.ProcessResponse.builder()
                    .processId(10L)
                    .applyId(1L)
                    .currentStep(ProcessStep.DOCUMENT_REVIEW)
                    .build();

            given(processService.transitionByApplyId(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/processes/applies/1/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.currentStep").value("DOCUMENT_REVIEW"));
        }
    }

    @Nested
    @DisplayName("프로세스 조회 API")
    class ReadApiTests {

        @Test
        @DisplayName("성공: 프로세스 상세 조회")
        void read_success() throws Exception {
            // given
            given(processService.read(10L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(get("/api/v1/processes/10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.processId").value(10L));
        }

        @Test
        @DisplayName("성공: 지원 ID로 프로세스 조회")
        void readByApply_success() throws Exception {
            // given
            given(processService.readByApplyId(1L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(get("/api/v1/processes/applies/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.applyId").value(1L));
        }

        @Test
        @DisplayName("성공: 공고별 프로세스 목록 조회")
        void getByJobposting_success() throws Exception {
            // given
            ProcessDto.ProcessPageResponse pageResponse = ProcessDto.ProcessPageResponse.builder()
                    .processes(List.of(createTestResponse()))
                    .processCount(1L)
                    .build();
            given(processService.getByJobposting(100L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/processes/jobpostings/100"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.processes").isArray())
                    .andExpect(jsonPath("$.result.processCount").value(1));
        }

        @Test
        @DisplayName("성공: 내 지원 현황 조회")
        void getByUser_success() throws Exception {
            // given
            ProcessDto.ProcessPageResponse pageResponse = ProcessDto.ProcessPageResponse.builder()
                    .processes(List.of(createTestResponse()))
                    .processCount(1L)
                    .build();
            given(processService.getByUser(200L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/processes/users/200"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.processes").isArray());
        }
    }

    @Nested
    @DisplayName("이력 조회 API")
    class HistoryApiTests {

        @Test
        @DisplayName("성공: 상태 변경 이력 조회")
        void getHistory_success() throws Exception {
            // given
            ProcessDto.HistoryResponse historyResponse = ProcessDto.HistoryResponse.builder()
                    .historyId(1L)
                    .processId(10L)
                    .applyId(1L)
                    .fromStep(ProcessStep.APPLIED)
                    .toStep(ProcessStep.DOCUMENT_REVIEW)
                    .reason("서류 검토 시작")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(processService.getHistory(10L)).willReturn(List.of(historyResponse));

            // when & then
            mockMvc.perform(get("/api/v1/processes/10/history"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result[0].fromStep").value("APPLIED"))
                    .andExpect(jsonPath("$.result[0].toStep").value("DOCUMENT_REVIEW"));
        }
    }

    @Nested
    @DisplayName("메타 정보 API")
    class MetaApiTests {

        @Test
        @DisplayName("성공: 모든 단계 정보 조회")
        void getAllSteps_success() throws Exception {
            // given
            ProcessDto.StepInfoResponse stepInfo = ProcessDto.StepInfoResponse.builder()
                    .step(ProcessStep.APPLIED)
                    .displayName("지원완료")
                    .allowedNextSteps(Set.of("DOCUMENT_REVIEW"))
                    .terminal(false)
                    .build();

            given(processService.getAllSteps()).willReturn(List.of(stepInfo));

            // when & then
            mockMvc.perform(get("/api/v1/processes/steps"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result[0].step").value("APPLIED"));
        }
    }

    @Nested
    @DisplayName("통계 API")
    class StatsApiTests {

        @Test
        @DisplayName("성공: 사용자 통계 조회")
        void getUserStats_success() throws Exception {
            // given
            ProcessDto.UserStatsResponse stats = ProcessDto.UserStatsResponse.builder()
                    .totalProcesses(10L)
                    .pendingProcesses(5L)
                    .passedProcesses(2L)
                    .failedProcesses(3L)
                    .passRate(40.0)
                    .build();

            given(processService.getUserStats(200L)).willReturn(stats);

            // when & then
            mockMvc.perform(get("/api/v1/processes/users/200/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalProcesses").value(10))
                    .andExpect(jsonPath("$.result.passRate").value(40.0));
        }

        @Test
        @DisplayName("성공: 공고별 통계 조회")
        void getJobpostingStats_success() throws Exception {
            // given
            ProcessDto.CompanyStatsResponse stats = ProcessDto.CompanyStatsResponse.builder()
                    .totalApplicants(50L)
                    .pendingApplicants(10L)
                    .interviewingApplicants(30L)
                    .passedApplicants(5L)
                    .failedApplicants(5L)
                    .passRate(50.0)
                    .build();

            given(processService.getJobpostingStats(100L)).willReturn(stats);

            // when & then
            mockMvc.perform(get("/api/v1/processes/jobpostings/100/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalApplicants").value(50))
                    .andExpect(jsonPath("$.result.passRate").value(50.0));
        }

        @Test
        @DisplayName("성공: 기업 전체 통계 조회")
        void getCompanyStats_success() throws Exception {
            // given
            List<Long> jobpostingIds = List.of(100L, 101L, 102L);

            ProcessDto.CompanyStatsResponse stats = ProcessDto.CompanyStatsResponse.builder()
                    .totalApplicants(150L)
                    .pendingApplicants(30L)
                    .interviewingApplicants(90L)
                    .passedApplicants(15L)
                    .failedApplicants(15L)
                    .passRate(50.0)
                    .build();

            given(processService.getCompanyStats(jobpostingIds)).willReturn(stats);

            // when & then
            mockMvc.perform(post("/api/v1/processes/company/stats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(jobpostingIds)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalApplicants").value(150));
        }
    }
}
