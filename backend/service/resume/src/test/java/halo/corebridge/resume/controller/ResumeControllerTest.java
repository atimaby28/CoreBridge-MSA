package halo.corebridge.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.resume.config.SecurityConfig;
import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.model.enums.ResumeStatus;
import halo.corebridge.resume.service.ResumeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ResumeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        SecurityConfig.class,
                        GatewayAuthenticationFilter.class,
                        AuditLoggingFilter.class
                }
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResumeService resumeService;

    private static final String BASE_URL = "/api/v1/resumes";

    private ResumeDto.ResumeResponse createMockResponse() {
        return ResumeDto.ResumeResponse.builder()
                .resumeId(1L)
                .userId(100L)
                .title("백엔드 개발자 이력서")
                .content("경력 사항...")
                .status(ResumeStatus.DRAFT)
                .currentVersion(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/resumes/me - 내 이력서 조회")
    class GetMyResumeTests {

        @Test
        @DisplayName("성공: 내 이력서 조회")
        void getMyResume_success() throws Exception {
            given(resumeService.getOrCreate(any())).willReturn(createMockResponse());

            mockMvc.perform(get(BASE_URL + "/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.title").value("백엔드 개발자 이력서"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/resumes/me - 내 이력서 업데이트")
    class UpdateMyResumeTests {

        @Test
        @DisplayName("성공: 이력서 업데이트")
        void updateMyResume_success() throws Exception {
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L).userId(100L).title("수정된 이력서").content("수정된 내용")
                    .status(ResumeStatus.DRAFT).currentVersion(2)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.update(any(), any(ResumeDto.UpdateRequest.class))).willReturn(response);

            String requestBody = """
                {"title": "수정된 이력서", "content": "수정된 내용"}
                """;

            mockMvc.perform(put(BASE_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.title").value("수정된 이력서"))
                    .andExpect(jsonPath("$.result.currentVersion").value(2));
        }
    }

    @Nested
    @DisplayName("버전 관리 API")
    class VersionTests {

        @Test
        @DisplayName("성공: 버전 목록 조회")
        void getVersions_success() throws Exception {
            ResumeDto.VersionResponse v1 = ResumeDto.VersionResponse.builder()
                    .versionId(1L).resumeId(1L).version(1).title("버전1")
                    .createdAt(LocalDateTime.now()).build();

            ResumeDto.VersionListResponse response = ResumeDto.VersionListResponse.builder()
                    .versions(List.of(v1)).totalCount(1).build();

            given(resumeService.getVersions(any())).willReturn(response);

            mockMvc.perform(get(BASE_URL + "/me/versions"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.versions").isArray())
                    .andExpect(jsonPath("$.result.totalCount").value(1));
        }

        @Test
        @DisplayName("성공: 버전 복원")
        void restoreVersion_success() throws Exception {
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L).userId(100L).title("복원된 이력서").content("복원된 내용")
                    .status(ResumeStatus.DRAFT).currentVersion(3)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.restoreVersion(any(), eq(1))).willReturn(response);

            mockMvc.perform(post(BASE_URL + "/me/versions/1/restore"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.title").value("복원된 이력서"));
        }
    }

    @Nested
    @DisplayName("AI 분석 API")
    class AiAnalysisTests {

        @Test
        @DisplayName("성공: AI 분석 요청")
        void requestAnalysis_success() throws Exception {
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L).userId(100L).title("이력서").content("내용")
                    .status(ResumeStatus.ANALYZING).currentVersion(1)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.requestAnalysis(any())).willReturn(response);

            mockMvc.perform(post(BASE_URL + "/me/analyze"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("ANALYZING"));
        }

        @Test
        @DisplayName("성공: AI 분석 결과 저장")
        void updateAiResult_success() throws Exception {
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L).userId(100L).title("이력서")
                    .status(ResumeStatus.ANALYZED).currentVersion(1)
                    .aiSummary("5년차 백엔드 개발자")
                    .aiSkills(List.of("Java", "Spring"))
                    .analyzedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.updateAiResult(eq(1L), any(ResumeDto.AiResultRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {"summary": "5년차 백엔드 개발자", "skills": "[\\"Java\\", \\"Spring\\"]"}
                """;

            mockMvc.perform(post(BASE_URL + "/1/ai-result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("ANALYZED"))
                    .andExpect(jsonPath("$.result.aiSummary").value("5년차 백엔드 개발자"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/resumes/by-user/{userId} - 사용자별 이력서 조회")
    class GetByUserTests {

        @Test
        @DisplayName("성공: userId로 이력서 조회")
        void getByUserId_success() throws Exception {
            given(resumeService.getOrCreate(100L)).willReturn(createMockResponse());

            mockMvc.perform(get(BASE_URL + "/by-user/100"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.userId").value(100));
        }
    }
}
