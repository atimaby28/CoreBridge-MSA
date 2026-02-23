package halo.corebridge.adminaudit.controller;

import halo.corebridge.adminaudit.model.dto.AuditDto;
import halo.corebridge.adminaudit.model.enums.AuditEventType;
import halo.corebridge.adminaudit.service.AuditService;
import halo.corebridge.common.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/audits")
public class AuditController {

    private final AuditService auditService;

    // ============================================
    // 로그 기록 (내부 서비스 호출용)
    // ============================================

    /**
     * 로그 기록
     */
    @PostMapping
    public BaseResponse<AuditDto.AuditResponse> log(@RequestBody AuditDto.CreateRequest request) {
        return BaseResponse.success(auditService.log(request));
    }

    // ============================================
    // 조회 (관리자 화면용)
    // ============================================

    /**
     * 최근 로그 조회
     */
    @GetMapping("/recent")
    public BaseResponse<AuditDto.AuditPageResponse> getRecent(
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        return BaseResponse.success(auditService.getRecent(size));
    }

    /**
     * 페이징된 로그 조회
     */
    @GetMapping
    public BaseResponse<AuditDto.AuditPageResponse> getRecentPaged(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return BaseResponse.success(auditService.getRecentPaged(page, size));
    }

    /**
     * 로그 상세 조회
     */
    @GetMapping("/{auditId}")
    public BaseResponse<AuditDto.AuditResponse> getById(@PathVariable("auditId") Long auditId) {
        return BaseResponse.success(auditService.getById(auditId));
    }

    /**
     * 사용자별 로그 조회
     */
    @GetMapping("/users/{userId}")
    public BaseResponse<List<AuditDto.AuditResponse>> getByUser(@PathVariable("userId") Long userId) {
        return BaseResponse.success(auditService.getByUser(userId));
    }

    /**
     * 서비스별 로그 조회
     */
    @GetMapping("/services/{serviceName}")
    public BaseResponse<List<AuditDto.AuditResponse>> getByService(
            @PathVariable("serviceName") String serviceName
    ) {
        return BaseResponse.success(auditService.getByService(serviceName));
    }

    /**
     * 이벤트 타입별 로그 조회
     */
    @GetMapping("/events/{eventType}")
    public BaseResponse<List<AuditDto.AuditResponse>> getByEventType(
            @PathVariable("eventType") AuditEventType eventType
    ) {
        return BaseResponse.success(auditService.getByEventType(eventType));
    }

    /**
     * 기간별 로그 조회
     */
    @GetMapping("/range")
    public BaseResponse<List<AuditDto.AuditResponse>> getByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return BaseResponse.success(auditService.getByDateRange(startDate, endDate));
    }

    /**
     * 에러 로그 조회
     */
    @GetMapping("/errors")
    public BaseResponse<List<AuditDto.AuditResponse>> getErrors(
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        return BaseResponse.success(auditService.getErrors(size));
    }

    // ============================================
    // 통계 (관리자 대시보드용)
    // ============================================

    /**
     * 통계 요약
     */
    @GetMapping("/stats")
    public BaseResponse<AuditDto.AuditStatsResponse> getStats() {
        return BaseResponse.success(auditService.getStats());
    }
}
