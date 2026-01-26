package halo.corebridge.schedule.service;

import halo.corebridge.common.exception.BaseException;
import halo.corebridge.common.response.BaseResponseStatus;
import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.schedule.client.NotificationClient;
import halo.corebridge.schedule.model.dto.ScheduleDto;
import halo.corebridge.schedule.model.entity.Schedule;
import halo.corebridge.schedule.model.enums.ScheduleStatus;
import halo.corebridge.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final Snowflake snowflake = new Snowflake();
    private final ScheduleRepository scheduleRepository;
    private final NotificationClient notificationClient;

    // ============================================
    // 일정 CRUD
    // ============================================

    /**
     * 일정 생성 (기업)
     */
    @Transactional
    public ScheduleDto.Response create(Long companyId, ScheduleDto.CreateRequest request) {
        // 시간 유효성 검증
        validateTimeRange(request.getStartTime(), request.getEndTime());

        // 충돌 체크
        ScheduleDto.ConflictCheckResponse conflictCheck = checkConflicts(
                request.getUserId(),
                request.getInterviewerId(),
                request.getStartTime(),
                request.getEndTime(),
                null  // 새로 생성이므로 제외할 ID 없음
        );

        if (conflictCheck.isHasConflict()) {
            throw new BaseException(BaseResponseStatus.SCHEDULE_CONFLICT);
        }

        Schedule schedule = Schedule.builder()
                .id(snowflake.nextId())
                .applyId(request.getApplyId())
                .jobpostingId(request.getJobpostingId())
                .userId(request.getUserId())
                .companyId(companyId)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .interviewerId(request.getInterviewerId())
                .interviewerName(request.getInterviewerName())
                .status(ScheduleStatus.SCHEDULED)
                .build();

        scheduleRepository.save(schedule);
        log.info("일정 생성: id={}, type={}, userId={}", schedule.getId(), schedule.getType(), schedule.getUserId());

        // 알림 전송
        notificationClient.sendScheduleCreatedNotification(schedule);

        return ScheduleDto.Response.from(schedule);
    }

    /**
     * 일정 상세 조회
     */
    @Transactional(readOnly = true)
    public ScheduleDto.Response read(Long scheduleId) {
        Schedule schedule = findById(scheduleId);
        return ScheduleDto.Response.from(schedule);
    }

    /**
     * 일정 수정 (기업)
     */
    @Transactional
    public ScheduleDto.Response update(Long scheduleId, Long companyId, ScheduleDto.UpdateRequest request) {
        Schedule schedule = findById(scheduleId);

        // 권한 확인
        if (!schedule.getCompanyId().equals(companyId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        // 시간 유효성 검증
        validateTimeRange(request.getStartTime(), request.getEndTime());

        // 충돌 체크 (자기 자신 제외)
        ScheduleDto.ConflictCheckResponse conflictCheck = checkConflicts(
                schedule.getUserId(),
                request.getInterviewerId(),
                request.getStartTime(),
                request.getEndTime(),
                scheduleId  // 자기 자신은 제외
        );

        if (conflictCheck.isHasConflict()) {
            throw new BaseException(BaseResponseStatus.SCHEDULE_CONFLICT);
        }

        schedule.update(
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getStartTime(),
                request.getEndTime(),
                request.getInterviewerId(),
                request.getInterviewerName()
        );

        log.info("일정 수정: id={}", scheduleId);

        // 알림 전송
        notificationClient.sendScheduleUpdatedNotification(schedule);

        return ScheduleDto.Response.from(schedule);
    }

    /**
     * 일정 상태 변경 (기업)
     */
    @Transactional
    public ScheduleDto.Response updateStatus(Long scheduleId, Long companyId, ScheduleDto.UpdateStatusRequest request) {
        Schedule schedule = findById(scheduleId);

        // 권한 확인
        if (!schedule.getCompanyId().equals(companyId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        ScheduleStatus oldStatus = schedule.getStatus();
        schedule.updateStatus(request.getStatus());

        log.info("일정 상태 변경: id={}, {} -> {}", scheduleId, oldStatus, request.getStatus());

        // 취소된 경우 알림 전송
        if (request.getStatus() == ScheduleStatus.CANCELLED) {
            notificationClient.sendScheduleCancelledNotification(schedule);
        }

        return ScheduleDto.Response.from(schedule);
    }

    /**
     * 일정 삭제 (기업)
     */
    @Transactional
    public void delete(Long scheduleId, Long companyId) {
        Schedule schedule = findById(scheduleId);

        // 권한 확인
        if (!schedule.getCompanyId().equals(companyId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        // 취소 알림 전송
        notificationClient.sendScheduleCancelledNotification(schedule);

        scheduleRepository.delete(schedule);
        log.info("일정 삭제: id={}", scheduleId);
    }

    /**
     * 메모 수정 (기업)
     */
    @Transactional
    public ScheduleDto.Response updateMemo(Long scheduleId, Long companyId, ScheduleDto.UpdateMemoRequest request) {
        Schedule schedule = findById(scheduleId);

        // 권한 확인
        if (!schedule.getCompanyId().equals(companyId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        schedule.updateMemo(request.getMemo());

        return ScheduleDto.Response.from(schedule);
    }

    // ============================================
    // 지원자용 조회
    // ============================================

    /**
     * 내 일정 목록 (지원자)
     */
    @Transactional(readOnly = true)
    public ScheduleDto.ListResponse getMySchedules(Long userId) {
        List<Schedule> schedules = scheduleRepository.findByUserIdOrderByStartTimeAsc(userId);

        List<ScheduleDto.Response> responses = schedules.stream()
                .map(ScheduleDto.Response::from)
                .toList();

        long upcoming = scheduleRepository.countByUserIdAndStatus(userId, ScheduleStatus.SCHEDULED);
        long completed = scheduleRepository.countByUserIdAndStatus(userId, ScheduleStatus.COMPLETED);

        return ScheduleDto.ListResponse.of(responses, upcoming, completed);
    }

    /**
     * 내 일정 (캘린더용)
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto.CalendarEventResponse> getMyCalendarEvents(
            Long userId, LocalDateTime start, LocalDateTime end) {

        List<Schedule> schedules = scheduleRepository.findByUserIdAndDateRange(userId, start, end);

        return schedules.stream()
                .map(ScheduleDto.CalendarEventResponse::from)
                .toList();
    }

    // ============================================
    // 기업용 조회
    // ============================================

    /**
     * 기업 일정 목록
     */
    @Transactional(readOnly = true)
    public ScheduleDto.ListResponse getCompanySchedules(Long companyId) {
        List<Schedule> schedules = scheduleRepository.findByCompanyIdOrderByStartTimeAsc(companyId);

        List<ScheduleDto.Response> responses = schedules.stream()
                .map(ScheduleDto.Response::from)
                .toList();

        long upcoming = scheduleRepository.countByCompanyIdAndStatus(companyId, ScheduleStatus.SCHEDULED);
        long completed = scheduleRepository.countByCompanyIdAndStatus(companyId, ScheduleStatus.COMPLETED);

        return ScheduleDto.ListResponse.of(responses, upcoming, completed);
    }

    /**
     * 기업 일정 (캘린더용)
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto.CalendarEventResponse> getCompanyCalendarEvents(
            Long companyId, LocalDateTime start, LocalDateTime end) {

        List<Schedule> schedules = scheduleRepository.findByCompanyIdAndDateRange(companyId, start, end);

        return schedules.stream()
                .map(ScheduleDto.CalendarEventResponse::from)
                .toList();
    }

    /**
     * 공고별 일정 조회
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto.Response> getSchedulesByJobposting(Long jobpostingId) {
        List<Schedule> schedules = scheduleRepository.findByJobpostingIdOrderByStartTimeAsc(jobpostingId);

        return schedules.stream()
                .map(ScheduleDto.Response::from)
                .toList();
    }

    /**
     * 지원서별 일정 조회
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto.Response> getSchedulesByApply(Long applyId) {
        List<Schedule> schedules = scheduleRepository.findByApplyIdOrderByStartTimeAsc(applyId);

        return schedules.stream()
                .map(ScheduleDto.Response::from)
                .toList();
    }

    // ============================================
    // 충돌 체크
    // ============================================

    /**
     * 일정 충돌 체크
     */
    @Transactional(readOnly = true)
    public ScheduleDto.ConflictCheckResponse checkConflicts(
            Long userId, Long interviewerId,
            LocalDateTime startTime, LocalDateTime endTime,
            Long excludeScheduleId) {

        List<ScheduleDto.ConflictCheckResponse.ConflictDetail> conflicts = new ArrayList<>();

        // 1. 지원자 충돌 체크
        List<Schedule> applicantConflicts = scheduleRepository.findApplicantConflicts(
                userId, startTime, endTime, excludeScheduleId);

        for (Schedule conflict : applicantConflicts) {
            conflicts.add(ScheduleDto.ConflictCheckResponse.ConflictDetail.builder()
                    .type("APPLICANT")
                    .scheduleId(conflict.getId())
                    .title(conflict.getTitle())
                    .startTime(conflict.getStartTime())
                    .endTime(conflict.getEndTime())
                    .message("지원자가 해당 시간에 다른 일정이 있습니다")
                    .build());
        }

        // 2. 면접관 충돌 체크 (면접관이 지정된 경우)
        if (interviewerId != null) {
            List<Schedule> interviewerConflicts = scheduleRepository.findInterviewerConflicts(
                    interviewerId, startTime, endTime, excludeScheduleId);

            for (Schedule conflict : interviewerConflicts) {
                conflicts.add(ScheduleDto.ConflictCheckResponse.ConflictDetail.builder()
                        .type("INTERVIEWER")
                        .scheduleId(conflict.getId())
                        .title(conflict.getTitle())
                        .startTime(conflict.getStartTime())
                        .endTime(conflict.getEndTime())
                        .message("면접관이 해당 시간에 다른 일정이 있습니다")
                        .build());
            }
        }

        return ScheduleDto.ConflictCheckResponse.builder()
                .hasConflict(!conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    // ============================================
    // Private 메서드
    // ============================================

    private Schedule findById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.SCHEDULE_NOT_FOUND));
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new BaseException(BaseResponseStatus.INVALID_TIME_RANGE);
        }
    }
}
