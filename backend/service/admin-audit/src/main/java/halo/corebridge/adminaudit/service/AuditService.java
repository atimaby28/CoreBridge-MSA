package halo.corebridge.adminaudit.service;

import halo.corebridge.adminaudit.model.dto.AuditDto;
import halo.corebridge.adminaudit.model.entity.AuditLog;
import halo.corebridge.adminaudit.model.enums.AuditEventType;
import halo.corebridge.adminaudit.repository.AuditLogRepository;
import halo.corebridge.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final Snowflake snowflake = new Snowflake();
    private final AuditLogRepository auditLogRepository;

    // ============================================
    // 로그 기록 (다른 서비스에서 호출)
    // ============================================

    @Transactional
    public AuditDto.AuditResponse log(AuditDto.CreateRequest request) {
        AuditLog auditLog = AuditLog.create(
                snowflake.nextId(),
                request.getUserId(),
                request.getUserEmail(),
                request.getServiceName(),
                request.getEventType(),
                request.getHttpMethod(),
                request.getRequestUri(),
                request.getClientIp(),
                request.getUserAgent(),
                request.getHttpStatus(),
                request.getExecutionTime(),
                request.getRequestBody(),
                request.getErrorMessage()
        );

        auditLogRepository.save(auditLog);

        return AuditDto.AuditResponse.from(auditLog);
    }

    // ============================================
    // 조회 (관리자 화면용)
    // ============================================

    /**
     * 최근 로그 조회
     */
    @Transactional(readOnly = true)
    public AuditDto.AuditPageResponse getRecent(int size) {
        List<AuditDto.AuditResponse> audits = auditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, size))
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();

        Long totalCount = auditLogRepository.count();

        return AuditDto.AuditPageResponse.of(audits, totalCount);
    }

    /**
     * 페이징된 로그 조회
     */
    @Transactional(readOnly = true)
    public AuditDto.AuditPageResponse getRecentPaged(int page, int size) {
        List<AuditDto.AuditResponse> audits = auditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();

        Long totalCount = auditLogRepository.count();

        return AuditDto.AuditPageResponse.of(audits, totalCount, page, size);
    }

    /**
     * 사용자별 로그 조회
     */
    @Transactional(readOnly = true)
    public List<AuditDto.AuditResponse> getByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();
    }

    /**
     * 서비스별 로그 조회
     */
    @Transactional(readOnly = true)
    public List<AuditDto.AuditResponse> getByService(String serviceName) {
        return auditLogRepository.findByServiceNameOrderByCreatedAtDesc(serviceName)
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();
    }

    /**
     * 이벤트 타입별 로그 조회
     */
    @Transactional(readOnly = true)
    public List<AuditDto.AuditResponse> getByEventType(AuditEventType eventType) {
        return auditLogRepository.findByEventTypeOrderByCreatedAtDesc(eventType)
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();
    }

    /**
     * 기간별 로그 조회
     */
    @Transactional(readOnly = true)
    public List<AuditDto.AuditResponse> getByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return auditLogRepository.findByDateRange(start, end)
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();
    }

    /**
     * 에러 로그만 조회
     */
    @Transactional(readOnly = true)
    public List<AuditDto.AuditResponse> getErrors(int size) {
        return auditLogRepository.findErrors(PageRequest.of(0, size))
                .stream()
                .map(AuditDto.AuditResponse::from)
                .toList();
    }

    /**
     * 로그 상세 조회
     */
    @Transactional(readOnly = true)
    public AuditDto.AuditResponse getById(Long auditId) {
        AuditLog auditLog = auditLogRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("로그를 찾을 수 없습니다."));

        return AuditDto.AuditResponse.from(auditLog);
    }

    // ============================================
    // 통계 (관리자 대시보드용)
    // ============================================

    /**
     * 통계 요약
     */
    @Transactional(readOnly = true)
    public AuditDto.AuditStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        Long totalRequests = auditLogRepository.count();
        Long errorCount = auditLogRepository.countErrors();
        Long uniqueUsers = auditLogRepository.countUniqueUsers(todayStart, todayEnd);
        Double avgExecutionTime = auditLogRepository.getAverageExecutionTime();

        // 가장 활발한 서비스
        List<Object[]> serviceStats = auditLogRepository.countByService();
        String mostActiveService = serviceStats.isEmpty() ? null : (String) serviceStats.get(0)[0];

        // 가장 빈번한 이벤트
        List<Object[]> eventStats = auditLogRepository.countByEventType();
        AuditEventType mostFrequentEvent = eventStats.isEmpty() ? null : (AuditEventType) eventStats.get(0)[0];

        return AuditDto.AuditStatsResponse.builder()
                .totalRequests(totalRequests)
                .errorCount(errorCount)
                .uniqueUsers(uniqueUsers)
                .avgExecutionTime(avgExecutionTime)
                .mostActiveService(mostActiveService)
                .mostFrequentEvent(mostFrequentEvent)
                .build();
    }
}
