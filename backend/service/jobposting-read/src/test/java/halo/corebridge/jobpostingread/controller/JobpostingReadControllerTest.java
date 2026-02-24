package halo.corebridge.jobpostingread.controller;

import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobpostingread.config.SecurityConfig;
import halo.corebridge.jobpostingread.model.dto.JobpostingReadDto;
import halo.corebridge.jobpostingread.service.JobpostingReadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = JobpostingReadController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.autoconfigure.exclude=")
@DisplayName("JobpostingReadController 테스트")
class JobpostingReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobpostingReadService jobpostingReadService;

    private JobpostingReadDto.Response createTestResponse() {
        return JobpostingReadDto.Response.builder()
                .jobpostingId(1L)
                .title("테스트 채용공고")
                .content("내용")
                .boardId(1L)
                .userId(100L)
                .nickname("테스터")
                .viewCount(150L)
                .likeCount(30L)
                .commentCount(10L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/jobposting-read/{jobpostingId} - 단일 조회 (통계 포함)")
    class ReadTest {

        @Test
        @DisplayName("성공: 통계가 포함된 채용공고를 조회한다")
        void read_success() throws Exception {
            // given
            given(jobpostingReadService.read(1L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-read/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.jobpostingId").value(1))
                    .andExpect(jsonPath("$.result.viewCount").value(150))
                    .andExpect(jsonPath("$.result.likeCount").value(30))
                    .andExpect(jsonPath("$.result.commentCount").value(10))
                    .andExpect(jsonPath("$.result.nickname").value("테스터"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채용공고 조회 시 예외 발생")
        void read_notFound_throwsException() throws Exception {
            // given
            given(jobpostingReadService.read(999L)).willThrow(new RuntimeException("Jobposting not found: 999"));

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-read/999"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jobposting-read - 목록 조회 (통계 포함)")
    class ReadAllTest {

        @Test
        @DisplayName("성공: 채용공고 목록을 통계와 함께 조회한다")
        void readAll_success() throws Exception {
            // given
            JobpostingReadDto.PageResponse pageResponse = JobpostingReadDto.PageResponse.builder()
                    .jobpostings(List.of(createTestResponse()))
                    .jobpostingCount(1L)
                    .build();
            given(jobpostingReadService.readAll(1L, 1L, 10L)).willReturn(pageResponse);

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-read")
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
        @DisplayName("성공: 빈 결과도 정상 응답한다")
        void readAll_empty_success() throws Exception {
            // given
            JobpostingReadDto.PageResponse emptyResponse = JobpostingReadDto.PageResponse.builder()
                    .jobpostings(List.of())
                    .jobpostingCount(0L)
                    .build();
            given(jobpostingReadService.readAll(99L, 1L, 10L)).willReturn(emptyResponse);

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-read")
                            .param("boardId", "99")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.jobpostings").isEmpty())
                    .andExpect(jsonPath("$.result.jobpostingCount").value(0));
        }
    }
}
