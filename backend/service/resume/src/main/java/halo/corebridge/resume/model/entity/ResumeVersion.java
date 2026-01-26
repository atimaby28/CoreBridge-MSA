package halo.corebridge.resume.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이력서 버전 엔티티
 * - 이력서 내용의 스냅샷 저장
 * - 이전 버전으로 복원 가능
 */
@Entity
@Table(indexes = {
    @Index(name = "idx_resume_version_resume_id", columnList = "resumeId"),
    @Index(name = "idx_resume_version_version", columnList = "resumeId, version")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long resumeId;

    @Column(nullable = false)
    private int version;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** 버전 저장 사유 (선택) */
    private String memo;

    // ============================================
    // Factory Method
    // ============================================

    public static ResumeVersion create(Long resumeId, int version, String title, String content) {
        ResumeVersion resumeVersion = new ResumeVersion();
        resumeVersion.resumeId = resumeId;
        resumeVersion.version = version;
        resumeVersion.title = title;
        resumeVersion.content = content;
        return resumeVersion;
    }

    public static ResumeVersion create(Long resumeId, int version, String title, String content, String memo) {
        ResumeVersion resumeVersion = create(resumeId, version, title, content);
        resumeVersion.memo = memo;
        return resumeVersion;
    }
}
