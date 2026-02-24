package halo.corebridge.jobpostinghot.controller;

import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobpostinghot.config.SecurityConfig;
import halo.corebridge.jobpostinghot.model.dto.JobpostingHotDto;
import halo.corebridge.jobpostinghot.service.JobpostingHotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = JobpostingHotController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("JobpostingHotController 테스트")
class JobpostingHotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobpostingHotService jobpostingHotService;

    private JobpostingHotDto.Response createTestResponse() {
        return JobpostingHotDto.Response.builder()
                .jobpostingId(1L)
                .title("인기 채용공고")
                .boardId(1L)
                .likeCount(50L)
                .commentCount(20L)
                .viewCount(500L)
                .score(50 * 3.0 + 20 * 2.0 + 500 * 1.0)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/jobposting-hot/today - 오늘의 인기 공고")
    class TodayTest {

        @Test
        @DisplayName("성공: 오늘의 인기 공고 TOP 10을 조회한다")
        void readToday_success() throws Exception {
            // given
            given(jobpostingHotService.readTopNWithLiveStats(10))
                    .willReturn(List.of(createTestResponse()));

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-hot/today"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result[0].jobpostingId").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/jobposting-hot/register/{jobpostingId} - 인기 공고 등록")
    class RegisterTest {

        @Test
        @DisplayName("성공: 인기 공고를 등록한다")
        void register_success() throws Exception {
            // given
            given(jobpostingHotService.register(1L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(post("/api/v1/jobposting-hot/register/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.jobpostingId").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/jobposting-hot/update-all - 전체 인기 공고 갱신")
    class UpdateAllTest {

        @Test
        @DisplayName("성공: 전체 인기 공고를 갱신한다")
        void updateAll_success() throws Exception {
            // given
            given(jobpostingHotService.updateAll()).willReturn(15);

            // when & then
            mockMvc.perform(post("/api/v1/jobposting-hot/update-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").value(15));
        }
    }
}
