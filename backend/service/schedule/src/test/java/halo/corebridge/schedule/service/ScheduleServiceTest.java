package halo.corebridge.schedule.service;

import halo.corebridge.common.exception.BaseException;
import halo.corebridge.schedule.client.NotificationClient;
import halo.corebridge.schedule.model.dto.ScheduleDto;
import halo.corebridge.schedule.model.entity.Schedule;
import halo.corebridge.schedule.model.enums.ScheduleStatus;
import halo.corebridge.schedule.model.enums.ScheduleType;
import halo.corebridge.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private ScheduleService scheduleService;

    private Long companyId;
    private Long userId;
    private Schedule testSchedule;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        companyId = 1L;
        userId = 2L;
        startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        endTime = startTime.plusHours(1);

        testSchedule = Schedule.builder()
                .id(100L)
                .applyId(10L)
                .jobpostingId(20L)
                .userId(userId)
                .companyId(companyId)
                .type(ScheduleType.INTERVIEW_1)
                .title("1차 면접")
                .description("기술 면접입니다")
                .location("본사 3층 회의실")
                .startTime(startTime)
                .endTime(endTime)
                .interviewerId(50L)
                .interviewerName("김면접")
                .status(ScheduleStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("일정 생성 성공")
    void createSchedule_Success() {
        // given
        ScheduleDto.CreateRequest request = ScheduleDto.CreateRequest.builder()
                .applyId(10L)
                .jobpostingId(20L)
                .userId(userId)
                .type(ScheduleType.INTERVIEW_1)
                .title("1차 면접")
                .description("기술 면접")
                .location("회의실")
                .startTime(startTime)
                .endTime(endTime)
                .interviewerId(50L)
                .interviewerName("김면접")
                .build();

        given(scheduleRepository.findApplicantConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.findInterviewerConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.save(any(Schedule.class))).willReturn(testSchedule);

        // when
        ScheduleDto.Response response = scheduleService.create(companyId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("1차 면접");
        assertThat(response.getType()).isEqualTo(ScheduleType.INTERVIEW_1);
        verify(notificationClient).sendScheduleCreatedNotification(any(Schedule.class));
    }

    @Test
    @DisplayName("일정 생성 실패 - 지원자 충돌")
    void createSchedule_ApplicantConflict_Fail() {
        // given
        ScheduleDto.CreateRequest request = ScheduleDto.CreateRequest.builder()
                .applyId(10L)
                .jobpostingId(20L)
                .userId(userId)
                .type(ScheduleType.INTERVIEW_1)
                .title("1차 면접")
                .startTime(startTime)
                .endTime(endTime)
                .build();

        given(scheduleRepository.findApplicantConflicts(any(), any(), any(), any()))
                .willReturn(List.of(testSchedule));

        // when & then
        assertThatThrownBy(() -> scheduleService.create(companyId, request))
                .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("일정 조회 성공")
    void readSchedule_Success() {
        // given
        given(scheduleRepository.findById(100L)).willReturn(Optional.of(testSchedule));

        // when
        ScheduleDto.Response response = scheduleService.read(100L);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("1차 면접");
    }

    @Test
    @DisplayName("일정 수정 성공")
    void updateSchedule_Success() {
        // given
        ScheduleDto.UpdateRequest request = ScheduleDto.UpdateRequest.builder()
                .title("1차 면접 (수정)")
                .description("기술 면접 - 시간 변경")
                .location("본사 5층 회의실")
                .startTime(startTime.plusHours(2))
                .endTime(endTime.plusHours(2))
                .interviewerId(50L)
                .interviewerName("김면접")
                .build();

        given(scheduleRepository.findById(100L)).willReturn(Optional.of(testSchedule));
        given(scheduleRepository.findApplicantConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.findInterviewerConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());

        // when
        ScheduleDto.Response response = scheduleService.update(100L, companyId, request);

        // then
        assertThat(response.getTitle()).isEqualTo("1차 면접 (수정)");
        verify(notificationClient).sendScheduleUpdatedNotification(any(Schedule.class));
    }

    @Test
    @DisplayName("일정 수정 실패 - 권한 없음")
    void updateSchedule_AccessDenied_Fail() {
        // given
        Long anotherCompanyId = 999L;
        ScheduleDto.UpdateRequest request = ScheduleDto.UpdateRequest.builder()
                .title("수정")
                .startTime(startTime)
                .endTime(endTime)
                .build();

        given(scheduleRepository.findById(100L)).willReturn(Optional.of(testSchedule));

        // when & then
        assertThatThrownBy(() -> scheduleService.update(100L, anotherCompanyId, request))
                .isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("일정 상태 변경 - 취소")
    void updateStatus_Cancel_Success() {
        // given
        ScheduleDto.UpdateStatusRequest request = new ScheduleDto.UpdateStatusRequest(ScheduleStatus.CANCELLED);
        given(scheduleRepository.findById(100L)).willReturn(Optional.of(testSchedule));

        // when
        ScheduleDto.Response response = scheduleService.updateStatus(100L, companyId, request);

        // then
        assertThat(response.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
        verify(notificationClient).sendScheduleCancelledNotification(any(Schedule.class));
    }

    @Test
    @DisplayName("충돌 체크 - 충돌 없음")
    void checkConflicts_NoConflict() {
        // given
        given(scheduleRepository.findApplicantConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.findInterviewerConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());

        // when
        ScheduleDto.ConflictCheckResponse response = scheduleService.checkConflicts(
                userId, 50L, startTime, endTime, null
        );

        // then
        assertThat(response.isHasConflict()).isFalse();
        assertThat(response.getConflicts()).isEmpty();
    }

    @Test
    @DisplayName("충돌 체크 - 면접관 충돌")
    void checkConflicts_InterviewerConflict() {
        // given
        given(scheduleRepository.findApplicantConflicts(any(), any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.findInterviewerConflicts(any(), any(), any(), any()))
                .willReturn(List.of(testSchedule));

        // when
        ScheduleDto.ConflictCheckResponse response = scheduleService.checkConflicts(
                userId, 50L, startTime, endTime, null
        );

        // then
        assertThat(response.isHasConflict()).isTrue();
        assertThat(response.getConflicts()).hasSize(1);
        assertThat(response.getConflicts().get(0).getType()).isEqualTo("INTERVIEWER");
    }

    @Test
    @DisplayName("내 일정 목록 조회")
    void getMySchedules_Success() {
        // given
        given(scheduleRepository.findByUserIdOrderByStartTimeAsc(userId))
                .willReturn(List.of(testSchedule));
        given(scheduleRepository.countByUserIdAndStatus(userId, ScheduleStatus.SCHEDULED))
                .willReturn(1L);
        given(scheduleRepository.countByUserIdAndStatus(userId, ScheduleStatus.COMPLETED))
                .willReturn(0L);

        // when
        ScheduleDto.ListResponse response = scheduleService.getMySchedules(userId);

        // then
        assertThat(response.getSchedules()).hasSize(1);
        assertThat(response.getUpcomingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("캘린더 이벤트 변환")
    void getCalendarEvents_Success() {
        // given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMonths(1);

        given(scheduleRepository.findByUserIdAndDateRange(userId, start, end))
                .willReturn(List.of(testSchedule));

        // when
        List<ScheduleDto.CalendarEventResponse> events = scheduleService.getMyCalendarEvents(userId, start, end);

        // then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTitle()).isEqualTo("1차 면접");
        assertThat(events.get(0).getColor()).isEqualTo("#3B82F6"); // INTERVIEW_1 색상
    }
}
