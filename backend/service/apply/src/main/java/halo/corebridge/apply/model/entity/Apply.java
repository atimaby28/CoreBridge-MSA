package halo.corebridge.apply.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 지원 정보
 *
 * 채용 공고에 대한 지원 기록을 저장합니다.
 * 상태(진행 단계)는 RecruitmentProcess에서 관리합니다.
 */
@Entity
@Table(
        name = "apply",
        uniqueConstraints = @UniqueConstraint(columnNames = {"jobpostingId", "userId"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Apply extends BaseTimeEntity {

    @Id
    private Long applyId;

    @Column(nullable = false)
    private Long jobpostingId;  // 지원한 공고

    @Column(nullable = false)
    private Long userId;        // 지원자

    private Long resumeId;      // 제출한 이력서

    private String memo;        // 기업 메모 (내부용)

    private String coverLetter; // 자기소개서 (간략)

    /**
     * 지원 생성 (팩토리 메서드)
     */
    public static Apply create(
            Long applyId,
            Long jobpostingId,
            Long userId,
            Long resumeId,
            String coverLetter
    ) {
        Apply apply = new Apply();
        apply.applyId = applyId;
        apply.jobpostingId = jobpostingId;
        apply.userId = userId;
        apply.resumeId = resumeId;
        apply.coverLetter = coverLetter;
        apply.createdAt = LocalDateTime.now();
        apply.updatedAt = apply.createdAt;
        return apply;
    }

    /**
     * 지원 생성 (이력서만)
     */
    public static Apply create(
            Long applyId,
            Long jobpostingId,
            Long userId,
            Long resumeId
    ) {
        return create(applyId, jobpostingId, userId, resumeId, null);
    }

    /**
     * 메모 수정 (기업 내부용)
     */
    public void updateMemo(String memo) {
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 이력서 변경
     */
    public void updateResume(Long resumeId) {
        this.resumeId = resumeId;
        this.updatedAt = LocalDateTime.now();
    }
}
