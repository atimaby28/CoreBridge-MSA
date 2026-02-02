package halo.corebridge.apply.model.dto;

import lombok.*;

import java.util.List;

public class AiMatchingDto {

    // ============================================
    // Request DTOs
    // ============================================

    /** 후보자 매칭 요청 (회사용) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchCandidatesRequest {
        private String jdText;
        private List<String> requiredSkills;
        private Integer topK;
    }

    /** 채용공고 매칭 요청 (구직자용) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchJobpostingsRequest {
        private String resumeText;
        private Integer topK;
    }

    /** 스코어 요청 (회사용) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreRequest {
        private String candidateId;
        private String jdText;
        private List<String> requiredSkills;
    }

    /** 스킬 갭 분석 요청 (구직자용) */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillGapRequest {
        private String candidateId;
        private String jobpostingId;
    }

    // ============================================
    // Response DTOs
    // ============================================

    /** 후보자 매칭 결과 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchCandidatesResponse {
        private List<MatchedCandidate> matches;
        private int totalCount;
    }

    /** 채용공고 매칭 결과 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchJobpostingsResponse {
        private List<MatchedJobposting> matches;
        private int totalCount;
    }

    /** 매칭된 후보자 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedCandidate {
        private String candidateId;
        private String userId;
        private String resumeId;
        private Double score;
        private String name;
        private List<String> skills;
    }

    /** 매칭된 채용공고 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedJobposting {
        private String jobpostingId;
        private Double score;
        private String title;
    }

    /** 스코어 결과 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreResponse {
        private String candidateId;
        private List<String> requiredSkills;
        private List<String> candidateSkills;
        private Double cosineSimilarity;
        private ScoreDetail scoreDetail;
    }

    /** 점수 상세 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDetail {
        private Double skillScore;
        private Double similarityScore;
        private Double bonusScore;
        private Double totalScore;
        private String grade;
    }

    /** 스킬 갭 분석 결과 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillGapResponse {
        private String candidateId;
        private String jobpostingId;
        private List<String> candidateSkills;
        private List<String> requiredSkills;
        private List<String> matchedSkills;
        private List<String> missingSkills;
        private Double matchRate;
        private Double cosineSimilarity;
    }
}
