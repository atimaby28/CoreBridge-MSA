package halo.corebridge.jobposting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobposting.config.SecurityConfig;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import halo.corebridge.jobposting.service.JobpostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = JobpostingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("JobpostingController 테스트")
class JobpostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobpostingService jobpostingService;

    private JobpostingDto.JobpostingResponse createTestResponse() {
        return JobpostingDto.JobpostingResponse.builder()
                .jobpostingId(1L)
                .title("백엔드 개발자 채용")
                .content("Spring Boot 경력자 우대")
                .boardId(1L)
                .userId(100L)
                .requiredSkills(List.of("Java", "Spring"))
                .preferredSkills(List.of("Kubernetes"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/jobpostings/{jobpostingId} - 단건 조회")
    class ReadTest {

        @Test
        @DisplayName("성공: 채용공고를 조회한다")
        void read_success() throws Exception {
            // given
            given(jobpostingService.read(1L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(get("/api/v1/jobpostings/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.jobpostingId").value(1))
                    .andExpect(jsonPath("$.result.title").value("백엔드 개발자 채용"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jobpostings - 목록 조회")
    class ReadAllTest {

        @Test
        @DisplayName("성공: 채용공고 목록을 페이징 조회한다")
        void readAll_success() throws Exception {
            // given
            JobpostingDto.JobpostingPageResponse pageResponse = JobpostingDto.JobpostingPageResponse.builder()
                    .jobpostings(List.of(createTestResponse()))
                    .jobpostingCount(1L)
                    .build();
            given(jobpostingService.readAll(1L, 1L, 10L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/jobpostings")
                            .param("boardId", "1")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.jobpostings").isArray())
                    .andExpect(jsonPath("$.result.jobpostingCount").value(1));
        }

        @Test
        @DisplayName("실패: 필수 파라미터 누락 시 에러 응답을 반환한다")
        void readAll_missingParams_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/jobpostings"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/jobpostings - 채용공고 생성")
    class CreateTest {

        @Test
        @DisplayName("성공: 채용공고를 생성한다")
        void create_success() throws Exception {
            // given
            String requestBody = """
                {
                    "title": "백엔드 개발자 채용",
                    "content": "Spring Boot 경력자 우대",
                    "boardId": 1,
                    "requiredSkills": ["Java", "Spring"],
                    "preferredSkills": ["Kubernetes"]
                }
                """;
            given(jobpostingService.create(any(), any())).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(post("/api/v1/jobpostings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.title").value("백엔드 개발자 채용"));
        }

        @Test
        @DisplayName("실패: 제목 없이 생성 요청 시 400 에러")
        void create_withoutTitle_returnsBadRequest() throws Exception {
            // given
            String requestBody = """
                {
                    "content": "내용만 있음",
                    "boardId": 1
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/jobpostings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/jobpostings/{jobpostingId} - 삭제")
    class DeleteTest {

        @Test
        @DisplayName("성공: 채용공고를 삭제한다")
        void delete_success() throws Exception {
            // given
            doNothing().when(jobpostingService).delete(any(), eq(1L));

            // when & then
            mockMvc.perform(delete("/api/v1/jobpostings/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
