package halo.corebridge.apply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.apply.config.SecurityConfig;
import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.security.JwtAuthenticationFilter;
import halo.corebridge.apply.security.JwtProvider;
import halo.corebridge.apply.service.ApplyService;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ApplyController.class,
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
class ApplyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplyService applyService;

    @MockitoBean
    private ProcessService processService;

    private ApplyDto.ApplyDetailResponse createTestResponse() {
        return ApplyDto.ApplyDetailResponse.builder()
                .applyId(1L)
                .jobpostingId(100L)
                .userId(200L)
                .resumeId(300L)
                .coverLetter("자기소개서")
                .appliedAt(LocalDateTime.now())
                .processId(10L)
                .currentStep(ProcessStep.APPLIED)
                .currentStepName("지원완료")
                .allowedNextSteps(Set.of("DOCUMENT_REVIEW"))
                .completed(false)
                .passed(false)
                .failed(false)
                .build();
    }

    @Nested
    @DisplayName("지원하기 API")
    class ApplyApiTests {

        @Test
        @DisplayName("성공: 지원 생성")
        void apply_success() throws Exception {
            // given
            ApplyDto.CreateRequest request = ApplyDto.CreateRequest.builder()
                    .jobpostingId(100L)
                    .userId(200L)
                    .resumeId(300L)
                    .coverLetter("열심히 하겠습니다.")
                    .build();

            given(applyService.apply(any())).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(post("/api/v1/applies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.applyId").value(1L))
                    .andExpect(jsonPath("$.result.currentStep").value("APPLIED"));
        }
    }

    @Nested
    @DisplayName("지원 취소 API")
    class CancelApiTests {

        @Test
        @DisplayName("성공: 지원 취소")
        void cancel_success() throws Exception {
            // given
            doNothing().when(applyService).cancel(1L, 200L);

            // when & then
            mockMvc.perform(delete("/api/v1/applies/1/users/200"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("지원 조회 API")
    class ReadApiTests {

        @Test
        @DisplayName("성공: 지원 상세 조회")
        void read_success() throws Exception {
            // given
            given(applyService.read(1L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(get("/api/v1/applies/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.applyId").value(1L));
        }

        @Test
        @DisplayName("성공: 내 지원 목록 조회")
        void getMyApplies_success() throws Exception {
            // given
            ApplyDto.ApplyPageResponse pageResponse = ApplyDto.ApplyPageResponse.builder()
                    .applies(List.of(createTestResponse()))
                    .totalCount(1L)
                    .build();
            given(applyService.getMyApplies(200L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/applies/users/200"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.applies").isArray())
                    .andExpect(jsonPath("$.result.totalCount").value(1));
        }

        @Test
        @DisplayName("성공: 공고별 지원자 목록 조회")
        void getAppliesByJobposting_success() throws Exception {
            // given
            ApplyDto.ApplyPageResponse pageResponse = ApplyDto.ApplyPageResponse.builder()
                    .applies(List.of(createTestResponse()))
                    .totalCount(1L)
                    .build();
            given(applyService.getAppliesByJobposting(100L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/applies/jobpostings/100"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.applies").isArray());
        }

        @Test
        @DisplayName("성공: 공고별 특정 단계 지원자 목록 조회")
        void getAppliesByStep_success() throws Exception {
            // given
            ApplyDto.ApplyPageResponse pageResponse = ApplyDto.ApplyPageResponse.builder()
                    .applies(List.of(createTestResponse()))
                    .totalCount(1L)
                    .build();
            given(applyService.getAppliesByStep(100L, ProcessStep.APPLIED)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/applies/jobpostings/100/steps/APPLIED"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("메모 수정 API")
    class UpdateMemoApiTests {

        @Test
        @DisplayName("성공: 메모 수정")
        void updateMemo_success() throws Exception {
            // given
            ApplyDto.UpdateMemoRequest request = ApplyDto.UpdateMemoRequest.builder()
                    .memo("면접 시 추가 질문 필요")
                    .build();

            ApplyDto.ApplyDetailResponse response = ApplyDto.ApplyDetailResponse.builder()
                    .applyId(1L)
                    .memo("면접 시 추가 질문 필요")
                    .currentStep(ProcessStep.APPLIED)
                    .build();

            given(applyService.updateMemo(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/applies/1/memo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.memo").value("면접 시 추가 질문 필요"));
        }
    }
}
