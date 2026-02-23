package halo.corebridge.adminaudit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.adminaudit.model.dto.AuditDto;
import halo.corebridge.adminaudit.model.enums.AuditEventType;
import halo.corebridge.adminaudit.service.AuditService;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
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
        controllers = AuditController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {AuditLoggingFilter.class, GatewayAuthenticationFilter.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuditController 테스트")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditService auditService;

    // ============================================
    // 테스트 데이터 헬퍼
    // ============================================

    private AuditDto.AuditResponse createAuditResponse(
            Long auditId, String serviceName, AuditEventType eventType,
            String httpMethod, String requestUri, Integer httpStatus
    ) {
        return AuditDto.AuditResponse.builder()
                .auditId(auditId)
                .userId(1001L)
                .userEmail("company@test.com")
                .serviceName(serviceName)
                .eventType(eventType)
                .eventTypeName(eventType.getDisplayName())
                .httpMethod(httpMethod)
                .requestUri(requestUri)
                .clientIp("127.0.0.1")
                .httpStatus(httpStatus)
                .executionTime(45L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================
    // POST /api/v1/admin/audits - 로그 기록
    // ============================================

    @Nested
    @DisplayName("POST /api/v1/admin/audits")
    class LogAudit {

        @Test
        @DisplayName("성공: 감사 로그를 기록한다")
        void log_success() throws Exception {
            // given
            AuditDto.CreateRequest request = AuditDto.CreateRequest.builder()
                    .userId(1001L)
                    .userEmail("company@test.com")
                    .serviceName("jobposting-read")
                    .eventType(AuditEventType.JOBPOSTING_READ)
                    .httpMethod("GET")
                    .requestUri("/api/v1/jobposting-read/123")
                    .clientIp("127.0.0.1")
                    .httpStatus(200)
                    .executionTime(45L)
                    .build();

            AuditDto.AuditResponse response = createAuditResponse(
                    1L, "jobposting-read", AuditEventType.JOBPOSTING_READ,
                    "GET", "/api/v1/jobposting-read/123", 200
            );

            given(auditService.log(any(AuditDto.CreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/admin/audits")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.serviceName").value("jobposting-read"))
                    .andExpect(jsonPath("$.result.eventType").value("JOBPOSTING_READ"))
                    .andExpect(jsonPath("$.result.httpStatus").value(200));
        }

        @Test
        @DisplayName("성공: 에러 로그를 기록한다")
        void log_error_event() throws Exception {
            // given
            AuditDto.CreateRequest request = AuditDto.CreateRequest.builder()
                    .userId(null)
                    .serviceName("user")
                    .eventType(AuditEventType.LOGIN_FAILED)
                    .httpMethod("POST")
                    .requestUri("/api/v1/users/login")
                    .clientIp("192.168.1.100")
                    .httpStatus(401)
                    .errorMessage("Invalid credentials")
                    .build();

            AuditDto.AuditResponse response = AuditDto.AuditResponse.builder()
                    .auditId(2L)
                    .serviceName("user")
                    .eventType(AuditEventType.LOGIN_FAILED)
                    .eventTypeName("로그인 실패")
                    .httpMethod("POST")
                    .requestUri("/api/v1/users/login")
                    .httpStatus(401)
                    .errorMessage("Invalid credentials")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(auditService.log(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/admin/audits")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.eventType").value("LOGIN_FAILED"))
                    .andExpect(jsonPath("$.result.httpStatus").value(401))
                    .andExpect(jsonPath("$.result.errorMessage").value("Invalid credentials"));
        }
    }

    // ============================================
    // GET /api/v1/admin/audits/recent - 최근 로그 조회
    // ============================================

    @Nested
    @DisplayName("GET /api/v1/admin/audits/recent")
    class GetRecent {

        @Test
        @DisplayName("성공: 최근 감사 로그 목록을 조회한다")
        void getRecent_success() throws Exception {
            // given
            List<AuditDto.AuditResponse> audits = List.of(
                    createAuditResponse(1L, "jobposting-read", AuditEventType.JOBPOSTING_READ, "GET", "/api/v1/jobposting-read/123", 200),
                    createAuditResponse(2L, "user", AuditEventType.LOGIN, "POST", "/api/v1/users/login", 200),
                    createAuditResponse(3L, "jobposting", AuditEventType.JOBPOSTING_CREATE, "POST", "/api/v1/jobpostings", 200)
            );

            given(auditService.getRecent(100))
                    .willReturn(AuditDto.AuditPageResponse.of(audits, 3L));

            // when & then
            mockMvc.perform(get("/api/v1/admin/audits/recent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.result.totalCount").value(3))
                    .andExpect(jsonPath("$.result.audits").isArray())
                    .andExpect(jsonPath("$.result.audits[0].serviceName").value("jobposting-read"))
                    .andExpect(jsonPath("$.result.audits[1].eventType").value("LOGIN"));
        }
    }

    // ============================================
    // GET /api/v1/admin/audits - 페이징 조회
    // ============================================

    @Nested
    @DisplayName("GET /api/v1/admin/audits (페이징)")
    class GetPaged {

        @Test
        @DisplayName("성공: 페이징된 감사 로그를 조회한다")
        void getPaged_success() throws Exception {
            // given
            List<AuditDto.AuditResponse> audits = List.of(
                    createAuditResponse(1L, "user", AuditEventType.USER_CREATE, "POST", "/api/v1/users/signup", 200)
            );

            given(auditService.getRecentPaged(0, 20))
                    .willReturn(AuditDto.AuditPageResponse.of(audits, 50L, 0, 20));

            // when & then
            mockMvc.perform(get("/api/v1/admin/audits")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.page").value(0))
                    .andExpect(jsonPath("$.result.size").value(20))
                    .andExpect(jsonPath("$.result.totalCount").value(50))
                    .andExpect(jsonPath("$.result.totalPages").value(3))
                    .andExpect(jsonPath("$.result.hasNext").value(true));
        }
    }

    // ============================================
    // GET /api/v1/admin/audits/services/{serviceName}
    // ============================================

    @Nested
    @DisplayName("GET /api/v1/admin/audits/services/{serviceName}")
    class GetByService {

        @Test
        @DisplayName("성공: 서비스별 로그를 조회한다")
        void getByService_success() throws Exception {
            // given
            List<AuditDto.AuditResponse> audits = List.of(
                    createAuditResponse(1L, "jobposting-read", AuditEventType.JOBPOSTING_READ, "GET", "/api/v1/jobposting-read/123", 200),
                    createAuditResponse(2L, "jobposting-read", AuditEventType.JOBPOSTING_READ, "GET", "/api/v1/jobposting-read", 200)
            );

            given(auditService.getByService("jobposting-read")).willReturn(audits);

            // when & then
            mockMvc.perform(get("/api/v1/admin/audits/services/jobposting-read"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(2))
                    .andExpect(jsonPath("$.result[0].serviceName").value("jobposting-read"));
        }
    }

    // ============================================
    // GET /api/v1/admin/audits/errors
    // ============================================

    @Nested
    @DisplayName("GET /api/v1/admin/audits/errors")
    class GetErrors {

        @Test
        @DisplayName("성공: 에러 로그만 조회한다")
        void getErrors_success() throws Exception {
            // given
            AuditDto.AuditResponse errorLog = AuditDto.AuditResponse.builder()
                    .auditId(10L)
                    .serviceName("user")
                    .eventType(AuditEventType.SYSTEM_ERROR)
                    .eventTypeName("시스템 오류")
                    .httpMethod("POST")
                    .requestUri("/api/v1/users/signup")
                    .httpStatus(500)
                    .errorMessage("Internal Server Error")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(auditService.getErrors(50)).willReturn(List.of(errorLog));

            // when & then
            mockMvc.perform(get("/api/v1/admin/audits/errors"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].httpStatus").value(500))
                    .andExpect(jsonPath("$.result[0].eventType").value("SYSTEM_ERROR"))
                    .andExpect(jsonPath("$.result[0].errorMessage").value("Internal Server Error"));
        }
    }

    // ============================================
    // GET /api/v1/admin/audits/stats - 통계
    // ============================================

    @Nested
    @DisplayName("GET /api/v1/admin/audits/stats")
    class GetStats {

        @Test
        @DisplayName("성공: 통계 요약을 조회한다")
        void getStats_success() throws Exception {
            // given
            AuditDto.AuditStatsResponse stats = AuditDto.AuditStatsResponse.builder()
                    .totalRequests(1250L)
                    .errorCount(23L)
                    .uniqueUsers(15L)
                    .avgExecutionTime(42.5)
                    .mostActiveService("jobposting-read")
                    .mostFrequentEvent(AuditEventType.JOBPOSTING_READ)
                    .build();

            given(auditService.getStats()).willReturn(stats);

            // when & then
            mockMvc.perform(get("/api/v1/admin/audits/stats"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalRequests").value(1250))
                    .andExpect(jsonPath("$.result.errorCount").value(23))
                    .andExpect(jsonPath("$.result.uniqueUsers").value(15))
                    .andExpect(jsonPath("$.result.avgExecutionTime").value(42.5))
                    .andExpect(jsonPath("$.result.mostActiveService").value("jobposting-read"))
                    .andExpect(jsonPath("$.result.mostFrequentEvent").value("JOBPOSTING_READ"));
        }
    }

    // ============================================
    // GET /api/v1/admin/audits/users/{userId}
    // ============================================

    @Nested
    @DisplayName("GET /api/v1/admin/audits/users/{userId}")
    class GetByUser {

        @Test
        @DisplayName("성공: 사용자별 로그를 조회한다")
        void getByUser_success() throws Exception {
            // given
            List<AuditDto.AuditResponse> audits = List.of(
                    createAuditResponse(1L, "user", AuditEventType.LOGIN, "POST", "/api/v1/users/login", 200),
                    createAuditResponse(2L, "jobposting", AuditEventType.JOBPOSTING_CREATE, "POST", "/api/v1/jobpostings", 200)
            );

            given(auditService.getByUser(1001L)).willReturn(audits);

            // when & then
            mockMvc.perform(get("/api/v1/admin/audits/users/1001"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result.length()").value(2));
        }
    }
}
