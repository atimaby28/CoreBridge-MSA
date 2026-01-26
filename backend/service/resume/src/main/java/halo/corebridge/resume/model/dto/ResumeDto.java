package halo.corebridge.resume.model.dto;

import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.model.entity.ResumeVersion;
import halo.corebridge.resume.model.enums.ResumeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ResumeDto {

    // ============================================
    // Request DTOs
    // ============================================

    @Getter
    public static class UpdateRequest {
        private String title;
        private String content;
        private String memo;  // 버전 저장 메모 (선택)
    }

    @Getter
    public static class AnalyzeRequest {
        private Long jobpostingId;  // 매칭할 공고 ID (선택)
    }

    @Getter
    public static class AiResultRequest {
        private String summary;
        private String skills;  // JSON 배열 문자열
        private Integer experienceYears;
    }

    // ============================================
    // Response DTOs
    // ============================================

    @Getter
    @Builder
    public static class ResumeResponse {
        private Long resumeId;
        private Long userId;
        private String title;
        private String content;
        private ResumeStatus status;
        private int currentVersion;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // AI 분석 결과
        private String aiSummary;
        private List<String> aiSkills;
        private Integer aiExperienceYears;
        private LocalDateTime analyzedAt;

        public static ResumeResponse from(Resume resume) {
            return ResumeResponse.builder()
                    .resumeId(resume.getId())
                    .userId(resume.getUserId())
                    .title(resume.getTitle())
                    .content(resume.getContent())
                    .status(resume.getStatus())
                    .currentVersion(resume.getCurrentVersion())
                    .createdAt(resume.getCreatedAt())
                    .updatedAt(resume.getUpdatedAt())
                    .aiSummary(resume.getAiSummary())
                    .aiSkills(parseSkills(resume.getAiSkills()))
                    .aiExperienceYears(resume.getAiExperienceYears())
                    .analyzedAt(resume.getAnalyzedAt())
                    .build();
        }

        private static List<String> parseSkills(String skillsJson) {
            if (skillsJson == null || skillsJson.isBlank()) {
                return List.of();
            }
            // 간단한 JSON 배열 파싱: ["Java", "Spring"] -> List<String>
            String cleaned = skillsJson.replaceAll("[\\[\\]\"]", "");
            if (cleaned.isBlank()) {
                return List.of();
            }
            return List.of(cleaned.split(",\\s*"));
        }
    }

    @Getter
    @Builder
    public static class VersionResponse {
        private Long versionId;
        private Long resumeId;
        private int version;
        private String title;
        private String content;
        private String memo;
        private LocalDateTime createdAt;

        public static VersionResponse from(ResumeVersion version) {
            return VersionResponse.builder()
                    .versionId(version.getId())
                    .resumeId(version.getResumeId())
                    .version(version.getVersion())
                    .title(version.getTitle())
                    .content(version.getContent())
                    .memo(version.getMemo())
                    .createdAt(version.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class VersionListResponse {
        private List<VersionResponse> versions;
        private int totalCount;
    }
}
