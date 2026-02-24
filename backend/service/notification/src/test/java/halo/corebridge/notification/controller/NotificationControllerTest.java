package halo.corebridge.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.notification.config.SecurityConfig;
import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.model.enums.NotificationType;
import halo.corebridge.notification.service.NotificationService;
import halo.corebridge.notification.service.SseEmitterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
        controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SseEmitterService sseEmitterService;

    private NotificationDto.Response createTestNotification() {
        return NotificationDto.Response.builder()
                .id(1L)
                .type(NotificationType.PROCESS_UPDATE)
                .title("지원 상태 변경")
                .message("서류 검토가 시작되었습니다")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/notifications - 알림 목록 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("성공: 내 알림 목록을 조회한다")
        void getMyNotifications_success() throws Exception {
            // given
            var page = new PageImpl<>(List.of(createTestNotification()), PageRequest.of(0, 20), 1);
            given(notificationService.getMyNotifications(any(), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/v1/notifications"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count - 읽지 않은 알림 수")
    class UnreadCountTest {

        @Test
        @DisplayName("성공: 읽지 않은 알림 개수를 반환한다")
        void getUnreadCount_success() throws Exception {
            // given
            given(notificationService.getUnreadCount(any()))
                    .willReturn(NotificationDto.UnreadCountResponse.of(5));

            // when & then
            mockMvc.perform(get("/api/v1/notifications/unread-count"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.count").value(5));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read - 읽음 처리")
    class MarkAsReadTest {

        @Test
        @DisplayName("성공: 알림을 읽음 처리한다")
        void markAsRead_success() throws Exception {
            // given
            given(notificationService.markAsRead(any(), eq(1L))).willReturn(true);

            // when & then
            mockMvc.perform(patch("/api/v1/notifications/1/read"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").value(true));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-all - 전체 읽음 처리")
    class MarkAllAsReadTest {

        @Test
        @DisplayName("성공: 모든 알림을 읽음 처리한다")
        void markAllAsRead_success() throws Exception {
            // given
            given(notificationService.markAllAsRead(any())).willReturn(10);

            // when & then
            mockMvc.perform(patch("/api/v1/notifications/read-all"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").value(10));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/recent - 최근 알림 조회")
    class RecentTest {

        @Test
        @DisplayName("성공: 최근 알림 10개를 조회한다")
        void getRecentNotifications_success() throws Exception {
            // given
            given(notificationService.getRecentNotifications(any()))
                    .willReturn(List.of(createTestNotification()));

            // when & then
            mockMvc.perform(get("/api/v1/notifications/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result").isArray());
        }
    }
}
