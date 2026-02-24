package halo.corebridge.jobpostinglike.controller;

import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobpostinglike.config.SecurityConfig;
import halo.corebridge.jobpostinglike.dto.JobpostingLikeResponse;
import halo.corebridge.jobpostinglike.service.JobpostingLikeService;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = JobpostingLikeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("JobpostingLikeController 테스트")
class JobpostingLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobpostingLikeService jobpostingLikeService;

    @Nested
    @DisplayName("GET /api/v1/jobposting-likes/jobpostings/{id}/count - 좋아요 수 조회")
    class CountTest {

        @Test
        @DisplayName("성공: 좋아요 수를 조회한다")
        void count_success() throws Exception {
            // given
            given(jobpostingLikeService.count(1L)).willReturn(42L);

            // when & then
            mockMvc.perform(get("/api/v1/jobposting-likes/jobpostings/1/count"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").value(42));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/jobposting-likes/jobpostings/{id} - 좋아요")
    class LikeTest {

        @Test
        @DisplayName("성공: 좋아요를 누른다")
        void like_success() throws Exception {
            // given
            doNothing().when(jobpostingLikeService).like(any(), any());

            // when & then
            mockMvc.perform(post("/api/v1/jobposting-likes/jobpostings/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/jobposting-likes/jobpostings/{id} - 좋아요 취소")
    class UnlikeTest {

        @Test
        @DisplayName("성공: 좋아요를 취소한다")
        void unlike_success() throws Exception {
            // given
            doNothing().when(jobpostingLikeService).unlike(any(), any());

            // when & then
            mockMvc.perform(delete("/api/v1/jobposting-likes/jobpostings/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
