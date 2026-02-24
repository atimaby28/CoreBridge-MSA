package halo.corebridge.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.notification.config.SecurityConfig;
import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.model.enums.NotificationType;
import halo.corebridge.notification.service.NotificationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = InternalNotificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InternalNotificationController 테스트")
class InternalNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @Nested
    @DisplayName("POST /internal/v1/notifications - 알림 생성")
    class CreateNotificationTest {

        @Test
        @DisplayName("성공: 내부 서비스에서 알림을 생성한다")
        void create_success() throws Exception {
            // given
            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .userId(1L)
                    .type(NotificationType.PROCESS_UPDATE)
                    .title("지원 상태 변경")
                    .message("서류 검토가 시작되었습니다")
                    .build();

            given(notificationService.create(any()))
                    .willReturn(NotificationDto.CreateResponse.success(100L));

            // when & then
            mockMvc.perform(post("/internal/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.id").value(100));
        }

        @Test
        @DisplayName("실패: 알림 생성 실패 시 400을 반환한다")
        void create_failure_returns400() throws Exception {
            // given
            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .userId(1L)
                    .type(NotificationType.SYSTEM)
                    .title("시스템 알림")
                    .message("테스트 메시지")
                    .build();

            given(notificationService.create(any()))
                    .willReturn(NotificationDto.CreateResponse.fail("알림 생성에 실패했습니다"));

            // when & then
            mockMvc.perform(post("/internal/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패: 필수 필드 누락 시 400을 반환한다")
        void create_missingFields_returns400() throws Exception {
            // given - title 없음
            String invalidJson = "{\"userId\":1,\"type\":\"PROCESS_UPDATE\",\"message\":\"메시지\"}";

            // when & then
            mockMvc.perform(post("/internal/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /internal/v1/notifications/health - 헬스체크")
    class HealthCheckTest {

        @Test
        @DisplayName("성공: OK를 반환한다")
        void health_success() throws Exception {
            mockMvc.perform(get("/internal/v1/notifications/health"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));
        }
    }
}
