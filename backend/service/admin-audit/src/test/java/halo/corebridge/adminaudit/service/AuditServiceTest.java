package halo.corebridge.adminaudit.service;

import halo.corebridge.adminaudit.model.dto.AuditDto;
import halo.corebridge.adminaudit.model.entity.AuditLog;
import halo.corebridge.adminaudit.model.enums.AuditEventType;
import halo.corebridge.adminaudit.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService 테스트")
class AuditServiceTest {

    @InjectMocks
    private AuditService auditService;

    @Mock
    private AuditLogRepository auditLogRepository;

    // ============================================
    // 로그 기록
    // ============================================

    @Nested
    @DisplayName("log()")
    class Log {

        @Test
        @DisplayName("성공: 감사 로그를 기록하고 응답을 반환한다")
        void log_success() {
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

            given(auditLogRepository.save(any(AuditLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            AuditDto.AuditResponse response = auditService.log(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getServiceName()).isEqualTo("jobposting-read");
            assertThat(response.getEventType()).isEqualTo(AuditEventType.JOBPOSTING_READ);
            assertThat(response.getHttpStatus()).isEqualTo(200);
            assertThat(response.getAuditId()).isNotNull();
            verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("성공: 에러 이벤트 로그를 기록한다")
        void log_error_event() {
            // given
            AuditDto.CreateRequest request = AuditDto.CreateRequest.builder()
                    .serviceName("user")
                    .eventType(AuditEventType.LOGIN_FAILED)
                    .httpMethod("POST")
                    .requestUri("/api/v1/users/login")
                    .httpStatus(401)
                    .errorMessage("Invalid credentials")
                    .build();

            given(auditLogRepository.save(any(AuditLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            AuditDto.AuditResponse response = auditService.log(request);

            // then
            assertThat(response.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
            assertThat(response.getHttpStatus()).isEqualTo(401);
            assertThat(response.getErrorMessage()).isEqualTo("Invalid credentials");
        }
    }

    // ============================================
    // 조회
    // ============================================

    @Nested
    @DisplayName("getRecent()")
    class GetRecent {

        @Test
        @DisplayName("성공: 최근 로그를 조회한다")
        void getRecent_success() {
            // given
            AuditLog log1 = AuditLog.create(
                    1L, 1001L, "company@test.com", "jobposting-read",
                    AuditEventType.JOBPOSTING_READ, "GET", "/api/v1/jobposting-read/123",
                    "127.0.0.1", null, 200, 45L, null, null
            );
            AuditLog log2 = AuditLog.create(
                    2L, 1001L, "company@test.com", "user",
                    AuditEventType.LOGIN, "POST", "/api/v1/users/login",
                    "127.0.0.1", null, 200, 120L, null, null
            );

            given(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100)))
                    .willReturn(List.of(log1, log2));
            given(auditLogRepository.count()).willReturn(2L);

            // when
            AuditDto.AuditPageResponse result = auditService.getRecent(100);

            // then
            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getAudits()).hasSize(2);
            assertThat(result.getAudits().get(0).getServiceName()).isEqualTo("jobposting-read");
        }
    }

    @Nested
    @DisplayName("getRecentPaged()")
    class GetRecentPaged {

        @Test
        @DisplayName("성공: 페이징된 로그를 조회한다")
        void getRecentPaged_success() {
            // given
            AuditLog log = AuditLog.createSimple(
                    1L, 1001L, "user", AuditEventType.USER_CREATE,
                    "POST", "/api/v1/users/signup", "127.0.0.1", 200
            );

            given(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                    .willReturn(List.of(log));
            given(auditLogRepository.count()).willReturn(50L);

            // when
            AuditDto.AuditPageResponse result = auditService.getRecentPaged(0, 20);

            // then
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getTotalCount()).isEqualTo(50);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getHasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("성공: ID로 로그를 조회한다")
        void getById_success() {
            // given
            AuditLog log = AuditLog.create(
                    1L, 1001L, "company@test.com", "jobposting",
                    AuditEventType.JOBPOSTING_CREATE, "POST", "/api/v1/jobpostings",
                    "127.0.0.1", "Mozilla/5.0", 200, 78L,
                    "{\"title\":\"Backend Developer\"}", null
            );

            given(auditLogRepository.findById(1L)).willReturn(Optional.of(log));

            // when
            AuditDto.AuditResponse result = auditService.getById(1L);

            // then
            assertThat(result.getAuditId()).isEqualTo(1L);
            assertThat(result.getEventType()).isEqualTo(AuditEventType.JOBPOSTING_CREATE);
            assertThat(result.getRequestBody()).contains("Backend Developer");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID로 조회하면 예외 발생")
        void getById_notFound() {
            // given
            given(auditLogRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> auditService.getById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("로그를 찾을 수 없습니다");
        }
    }

    // ============================================
    // 통계
    // ============================================

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("성공: 통계 요약을 반환한다")
        void getStats_success() {
            // given
            given(auditLogRepository.count()).willReturn(1250L);
            given(auditLogRepository.countErrors()).willReturn(23L);
            given(auditLogRepository.countUniqueUsers(any(), any())).willReturn(15L);
            given(auditLogRepository.getAverageExecutionTime()).willReturn(42.5);
            List<Object[]> serviceStats = new java.util.ArrayList<>();
            serviceStats.add(new Object[]{"jobposting-read", 500L});
            given(auditLogRepository.countByService()).willReturn(serviceStats);

            List<Object[]> eventStats = new java.util.ArrayList<>();
            eventStats.add(new Object[]{AuditEventType.JOBPOSTING_READ, 400L});
            given(auditLogRepository.countByEventType()).willReturn(eventStats);

            // when
            AuditDto.AuditStatsResponse result = auditService.getStats();

            // then
            assertThat(result.getTotalRequests()).isEqualTo(1250);
            assertThat(result.getErrorCount()).isEqualTo(23);
            assertThat(result.getUniqueUsers()).isEqualTo(15);
            assertThat(result.getAvgExecutionTime()).isEqualTo(42.5);
            assertThat(result.getMostActiveService()).isEqualTo("jobposting-read");
            assertThat(result.getMostFrequentEvent()).isEqualTo(AuditEventType.JOBPOSTING_READ);
        }
    }
}
