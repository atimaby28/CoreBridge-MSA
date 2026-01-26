package halo.corebridge.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.model.enums.ResumeStatus;
import halo.corebridge.resume.service.ResumeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
        @WithMockUser(username = "100", roles = "USER")
        void getMyResume_success() throws Exception {
            // given
            given(resumeService.getOrCreate(any())).willReturn(createMockResponse());

            // when & then
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.title").value("백엔드 개발자 이력서"));
        }

        @Test
        @DisplayName("실패: 인증 없이 접근")
        void getMyResume_unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/resumes/me - 내 이력서 업데이트")
    class UpdateMyResumeTests {

        @Test
        @DisplayName("성공: 이력서 업데이트")
        @WithMockUser(username = "100", roles = "USER")
        void updateMyResume_success() throws Exception {
            // given
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L)
                    .userId(100L)
                    .title("수정된 이력서")
                    .content("수정된 내용")
                    .status(ResumeStatus.DRAFT)
                    .currentVersion(2)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.update(any(), any(ResumeDto.UpdateRequest.class))).willReturn(response);

            String requestBody = """
                {
                    "title": "수정된 이력서",
                    "content": "수정된 내용"
                }
                """;

            // when & then
            mockMvc.perform(put(BASE_URL + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.title").value("수정된 이력서"))
                    .andExpect(jsonPath("$.result.currentVersion").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/resumes/me/versions - 버전 목록 조회")
    class GetVersionsTests {

        @Test
        @DisplayName("성공: 버전 목록 조회")
        @WithMockUser(username = "100", roles = "USER")
        void getVersions_success() throws Exception {
            // given
            ResumeDto.VersionResponse v1 = ResumeDto.VersionResponse.builder()
                    .versionId(1L)
                    .resumeId(1L)
                    .version(1)
                    .title("버전1")
                    .createdAt(LocalDateTime.now())
                    .build();

            ResumeDto.VersionListResponse response = ResumeDto.VersionListResponse.builder()
                    .versions(List.of(v1))
                    .totalCount(1)
                    .build();

            given(resumeService.getVersions(any())).willReturn(response);

            // when & then
            mockMvc.perform(get(BASE_URL + "/me/versions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.versions").isArray())
                    .andExpect(jsonPath("$.result.totalCount").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/resumes/me/versions/{version}/restore - 버전 복원")
    class RestoreVersionTests {

        @Test
        @DisplayName("성공: 버전 복원")
        @WithMockUser(username = "100", roles = "USER")
        void restoreVersion_success() throws Exception {
            // given
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L)
                    .userId(100L)
                    .title("복원된 이력서")
                    .content("복원된 내용")
                    .status(ResumeStatus.DRAFT)
                    .currentVersion(3)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.restoreVersion(any(), eq(1))).willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/me/versions/1/restore"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.title").value("복원된 이력서"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/resumes/me/analyze - AI 분석 요청")
    class AnalyzeTests {

        @Test
        @DisplayName("성공: AI 분석 요청")
        @WithMockUser(username = "100", roles = "USER")
        void requestAnalysis_success() throws Exception {
            // given
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L)
                    .userId(100L)
                    .title("이력서")
                    .content("내용")
                    .status(ResumeStatus.ANALYZING)
                    .currentVersion(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.requestAnalysis(any())).willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL + "/me/analyze"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.status").value("ANALYZING"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/resumes/{resumeId}/ai-result - AI 결과 콜백")
    class AiResultCallbackTests {

        @Test
        @DisplayName("성공: AI 분석 결과 저장")
        @WithMockUser(username = "100", roles = "USER")
        void updateAiResult_success() throws Exception {
            // given
            ResumeDto.ResumeResponse response = ResumeDto.ResumeResponse.builder()
                    .resumeId(1L)
                    .userId(100L)
                    .title("이력서")
                    .status(ResumeStatus.ANALYZED)
                    .currentVersion(1)
                    .aiSummary("5년차 백엔드 개발자")
                    .aiSkills(List.of("Java", "Spring"))
                    .aiExperienceYears(5)
                    .analyzedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(resumeService.updateAiResult(eq(1L), any(ResumeDto.AiResultRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "summary": "5년차 백엔드 개발자",
                    "skills": "[\\"Java\\", \\"Spring\\"]",
                    "experienceYears": 5
                }
                """;

            // when & then
            mockMvc.perform(post(BASE_URL + "/1/ai-result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.status").value("ANALYZED"))
                    .andExpect(jsonPath("$.result.aiSummary").value("5년차 백엔드 개발자"));
        }
    }
}
