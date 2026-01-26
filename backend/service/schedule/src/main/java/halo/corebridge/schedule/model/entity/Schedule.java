package halo.corebridge.schedule.model.entity;

import halo.corebridge.schedule.model.enums.ScheduleStatus;
import halo.corebridge.schedule.model.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_schedule_user", columnList = "userId"),
        @Index(name = "idx_schedule_company", columnList = "companyId"),
        @Index(name = "idx_schedule_jobposting", columnList = "jobpostingId"),
        @Index(name = "idx_schedule_apply", columnList = "applyId"),
        @Index(name = "idx_schedule_interviewer", columnList = "interviewerId"),
        @Index(name = "idx_schedule_time", columnList = "startTime, endTime")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long applyId;

    @Column(nullable = false)
    private Long jobpostingId;

    @Column(nullable = false)
    private Long userId;  // 지원자 ID

    @Column(nullable = false)
    private Long companyId;  // 기업 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String location;  // 장소 또는 온라인 링크

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private Long interviewerId;  // 면접관 ID (nullable)

    @Column(length = 50)
    private String interviewerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String memo;  // 기업 내부 메모

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ============================================
    // 비즈니스 메서드
    // ============================================

    public void update(String title, String description, String location,
                       LocalDateTime startTime, LocalDateTime endTime,
                       Long interviewerId, String interviewerName) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.interviewerId = interviewerId;
        this.interviewerName = interviewerName;
    }

    public void updateStatus(ScheduleStatus status) {
        this.status = status;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void cancel() {
        this.status = ScheduleStatus.CANCELLED;
    }

    public void complete() {
        this.status = ScheduleStatus.COMPLETED;
    }

    public void markNoShow() {
        this.status = ScheduleStatus.NO_SHOW;
    }

    public boolean isUpcoming() {
        return this.status == ScheduleStatus.SCHEDULED &&
                this.startTime.isAfter(LocalDateTime.now());
    }

    public boolean isPast() {
        return this.endTime.isBefore(LocalDateTime.now());
    }
}
