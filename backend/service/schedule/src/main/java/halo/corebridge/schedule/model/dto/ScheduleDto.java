package halo.corebridge.schedule.model.dto;

import halo.corebridge.schedule.model.entity.Schedule;
import halo.corebridge.schedule.model.enums.ScheduleStatus;
import halo.corebridge.schedule.model.enums.ScheduleType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduleDto {

    // ============================================
    // Request DTOs
    // ============================================

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotNull(message = "지원 ID는 필수입니다")
        private Long applyId;

        @NotNull(message = "공고 ID는 필수입니다")
        private Long jobpostingId;

        @NotNull(message = "지원자 ID는 필수입니다")
        private Long userId;

        @NotNull(message = "일정 유형은 필수입니다")
        private ScheduleType type;

        @NotBlank(message = "제목은 필수입니다")
        private String title;

        private String description;

        private String location;

        @NotNull(message = "시작 시간은 필수입니다")
        @Future(message = "시작 시간은 미래여야 합니다")
        private LocalDateTime startTime;

        @NotNull(message = "종료 시간은 필수입니다")
        @Future(message = "종료 시간은 미래여야 합니다")
        private LocalDateTime endTime;

        private Long interviewerId;

        private String interviewerName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {

        @NotBlank(message = "제목은 필수입니다")
        private String title;

        private String description;

        private String location;

        @NotNull(message = "시작 시간은 필수입니다")
        private LocalDateTime startTime;

        @NotNull(message = "종료 시간은 필수입니다")
        private LocalDateTime endTime;

        private Long interviewerId;

        private String interviewerName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotNull(message = "상태는 필수입니다")
        private ScheduleStatus status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateMemoRequest {
        private String memo;
    }

    // ============================================
    // Response DTOs
    // ============================================

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long applyId;
        private Long jobpostingId;
        private Long userId;
        private Long companyId;
        private ScheduleType type;
        private String typeDescription;
        private String title;
        private String description;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long interviewerId;
        private String interviewerName;
        private ScheduleStatus status;
        private String statusDescription;
        private String memo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Schedule schedule) {
            return Response.builder()
                    .id(schedule.getId())
                    .applyId(schedule.getApplyId())
                    .jobpostingId(schedule.getJobpostingId())
                    .userId(schedule.getUserId())
                    .companyId(schedule.getCompanyId())
                    .type(schedule.getType())
                    .typeDescription(schedule.getType().getDescription())
                    .title(schedule.getTitle())
                    .description(schedule.getDescription())
                    .location(schedule.getLocation())
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .interviewerId(schedule.getInterviewerId())
                    .interviewerName(schedule.getInterviewerName())
                    .status(schedule.getStatus())
                    .statusDescription(schedule.getStatus().getDescription())
                    .memo(schedule.getMemo())
                    .createdAt(schedule.getCreatedAt())
                    .updatedAt(schedule.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 캘린더 UI용 이벤트 응답
     * FullCalendar 형식에 맞춤
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarEventResponse {
        private String id;
        private String title;
        private String start;  // ISO 8601 format
        private String end;
        private String color;
        private String backgroundColor;
        private String borderColor;
        private String textColor;
        private boolean allDay;
        private ExtendedProps extendedProps;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ExtendedProps {
            private Long scheduleId;
            private Long applyId;
            private Long jobpostingId;
            private Long userId;
            private ScheduleType type;
            private String typeDescription;
            private ScheduleStatus status;
            private String statusDescription;
            private String location;
            private String description;
            private Long interviewerId;
            private String interviewerName;
        }

        public static CalendarEventResponse from(Schedule schedule) {
            String color = getColorByType(schedule.getType());

            return CalendarEventResponse.builder()
                    .id(String.valueOf(schedule.getId()))
                    .title(schedule.getTitle())
                    .start(schedule.getStartTime().toString())
                    .end(schedule.getEndTime().toString())
                    .color(color)
                    .backgroundColor(color)
                    .borderColor(color)
                    .textColor("#ffffff")
                    .allDay(false)
                    .extendedProps(ExtendedProps.builder()
                            .scheduleId(schedule.getId())
                            .applyId(schedule.getApplyId())
                            .jobpostingId(schedule.getJobpostingId())
                            .userId(schedule.getUserId())
                            .type(schedule.getType())
                            .typeDescription(schedule.getType().getDescription())
                            .status(schedule.getStatus())
                            .statusDescription(schedule.getStatus().getDescription())
                            .location(schedule.getLocation())
                            .description(schedule.getDescription())
                            .interviewerId(schedule.getInterviewerId())
                            .interviewerName(schedule.getInterviewerName())
                            .build())
                    .build();
        }

        private static String getColorByType(ScheduleType type) {
            return switch (type) {
                case CODING_TEST -> "#8B5CF6";      // 보라색
                case INTERVIEW_1 -> "#3B82F6";     // 파란색
                case INTERVIEW_2 -> "#10B981";     // 초록색
                case FINAL_INTERVIEW -> "#F59E0B"; // 주황색
                case ORIENTATION -> "#EC4899";     // 핑크색
                case OTHER -> "#6B7280";           // 회색
            };
        }
    }

    /**
     * 일정 충돌 체크 응답
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConflictCheckResponse {
        private boolean hasConflict;
        private List<ConflictDetail> conflicts;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ConflictDetail {
            private String type;  // INTERVIEWER, APPLICANT
            private Long scheduleId;
            private String title;
            private LocalDateTime startTime;
            private LocalDateTime endTime;
            private String message;
        }
    }

    /**
     * 일정 목록 응답
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private List<Response> schedules;
        private long totalCount;
        private long upcomingCount;
        private long completedCount;

        public static ListResponse of(List<Response> schedules, long upcoming, long completed) {
            return ListResponse.builder()
                    .schedules(schedules)
                    .totalCount(schedules.size())
                    .upcomingCount(upcoming)
                    .completedCount(completed)
                    .build();
        }
    }
}
