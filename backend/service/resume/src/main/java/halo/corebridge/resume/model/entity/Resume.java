package halo.corebridge.resume.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import halo.corebridge.resume.model.enums.ResumeStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이력서 엔티티
 * - 사용자당 1개의 이력서만 존재
 * - AI 분석 결과 저장
 * - 버전 관리는 ResumeVersion 엔티티에서 담당
 */
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = "userId")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private ResumeStatus status;

    private int currentVersion;

    // ============================================
    // AI 분석 결과 필드
    // ============================================
    
    /** AI가 생성한 이력서 요약 */
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    /** AI가 추출한 스킬 목록 (JSON 배열 형태) */
    @Column(columnDefinition = "TEXT")
    private String aiSkills;

    /** AI가 추출한 경력 연차 */
    private Integer aiExperienceYears;

    /** 마지막 AI 분석 시각 */
    private LocalDateTime analyzedAt;

    // ============================================
    // Factory Methods
    // ============================================

    public static Resume create(Long userId) {
        Resume resume = new Resume();
        resume.userId = userId;
        resume.title = "내 이력서";
        resume.status = ResumeStatus.DRAFT;
        resume.currentVersion = 1;
        return resume;
    }

    public static Resume create(Long userId, String title, String content) {
        Resume resume = new Resume();
        resume.userId = userId;
        resume.title = title;
        resume.content = content;
        resume.status = ResumeStatus.DRAFT;
        resume.currentVersion = 1;
        return resume;
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * 이력서 내용 업데이트 (버전 증가)
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.currentVersion++;
        // 내용이 변경되면 AI 분석 결과 초기화
        clearAiAnalysis();
    }

    /**
     * 특정 버전으로 복원
     */
    public void restoreFromVersion(String title, String content, int newVersion) {
        this.title = title;
        this.content = content;
        this.currentVersion = newVersion;
        clearAiAnalysis();
    }

    /**
     * AI 분석 시작
     */
    public void markAnalyzing() {
        this.status = ResumeStatus.ANALYZING;
    }

    /**
     * AI 분석 결과 저장
     */
    public void updateAiAnalysis(String summary, String skills, Integer experienceYears) {
        this.aiSummary = summary;
        this.aiSkills = skills;
        this.aiExperienceYears = experienceYears;
        this.analyzedAt = LocalDateTime.now();
        this.status = ResumeStatus.ANALYZED;
    }

    /**
     * AI 분석 결과 초기화
     */
    public void clearAiAnalysis() {
        this.aiSummary = null;
        this.aiSkills = null;
        this.aiExperienceYears = null;
        this.analyzedAt = null;
        this.status = ResumeStatus.DRAFT;
    }

    /**
     * 소프트 삭제
     */
    public void delete() {
        this.status = ResumeStatus.DELETED;
    }
}
