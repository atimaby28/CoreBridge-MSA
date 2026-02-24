package halo.corebridge.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.schedule.config.SecurityConfig;
import halo.corebridge.schedule.model.dto.ScheduleDto;
import halo.corebridge.schedule.model.enums.ScheduleStatus;
import halo.corebridge.schedule.model.enums.ScheduleType;
import halo.corebridge.schedule.service.ScheduleService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ScheduleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ScheduleController 테스트")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;

    private ScheduleDto.Response createTestResponse() {
        return ScheduleDto.Response.builder()
                .id(1L)
                .applyId(10L)
                .jobpostingId(100L)
                .userId(200L)
                .companyId(300L)
                .type(ScheduleType.INTERVIEW_1)
                .title("1차 면접")
                .description("기술 면접입니다")
                .location("서울 강남구")
                .startTime(LocalDateTime.of(2026, 3, 1, 14, 0))
                .endTime(LocalDateTime.of(2026, 3, 1, 15, 0))
                .status(ScheduleStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/schedules/{scheduleId} - 일정 상세 조회")
    class ReadTest {

        @Test
        @DisplayName("성공: 일정을 조회한다")
        void getSchedule_success() throws Exception {
            // given
            given(scheduleService.read(1L)).willReturn(createTestResponse());

            // when & then
            mockMvc.perform(get("/api/v1/schedules/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.id").value(1))
                    .andExpect(jsonPath("$.result.title").value("1차 면접"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/schedules/my - 내 일정 목록")
    class MySchedulesTest {

        @Test
        @DisplayName("성공: 내 일정 목록을 조회한다")
        void getMySchedules_success() throws Exception {
            // given
            ScheduleDto.ListResponse listResponse = ScheduleDto.ListResponse.of(
                    List.of(createTestResponse()), 1, 0);
            given(scheduleService.getMySchedules(any())).willReturn(listResponse);

            // when & then
            mockMvc.perform(get("/api/v1/schedules/my"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.schedules").isArray())
                    .andExpect(jsonPath("$.result.totalCount").value(1));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/schedules/{scheduleId} - 일정 삭제")
    class DeleteTest {

        @Test
        @DisplayName("성공: 일정을 삭제한다")
        void deleteSchedule_success() throws Exception {
            // given
            doNothing().when(scheduleService).delete(eq(1L), any());

            // when & then
            mockMvc.perform(delete("/api/v1/schedules/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/schedules/jobposting/{jobpostingId} - 공고별 일정")
    class ByJobpostingTest {

        @Test
        @DisplayName("성공: 공고별 일정을 조회한다")
        void getSchedulesByJobposting_success() throws Exception {
            // given
            given(scheduleService.getSchedulesByJobposting(100L))
                    .willReturn(List.of(createTestResponse()));

            // when & then
            mockMvc.perform(get("/api/v1/schedules/jobposting/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").isArray());
        }
    }
}
