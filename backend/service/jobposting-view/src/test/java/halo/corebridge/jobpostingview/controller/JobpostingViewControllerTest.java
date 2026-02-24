package halo.corebridge.jobpostingview.controller;

import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobpostingview.config.SecurityConfig;
import halo.corebridge.jobpostingview.service.JobpostingViewService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = JobpostingViewController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("JobpostingViewController 테스트")
class JobpostingViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobpostingViewService jobpostingViewService;

    @Nested
    @DisplayName("POST /api/v1/jobposting-views/jobpostings/{id} - 조회수 증가")
    class IncreaseTest {

        @Test
        @DisplayName("성공: 조회수를 증가시킨다")
        void increase_success() throws Exception {
            // given
            given(jobpostingViewService.increase(any(), any())).willReturn(101L);

            // when & then
            mockMvc.perform(post("/api/v1/jobposting-views/jobpostings/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").value(101));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/jobposting-views/jobpostings/{id}/count - 조회수 조회")
    class CountTest {

        @Test
        @DisplayName("성공: 조회수를 조회한다")
        void count_success() throws Exception {
            // given
            given(jobpostingViewService.count(1L)).willReturn(500L);

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-views/jobpostings/1/count"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").value(500));
        }

        @Test
        @DisplayName("성공: 조회수가 0인 경우")
        void count_zero_success() throws Exception {
            // given
            given(jobpostingViewService.count(999L)).willReturn(0L);

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-views/jobpostings/999/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value(0));
        }
    }
}
