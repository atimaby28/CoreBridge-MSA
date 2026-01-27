package halo.corebridge.apply.model.dto;

import lombok.*;

import java.util.List;

public class AiMatchingDto {

    // ============================================
    // Request DTOs
    // ============================================

    /**
     * JD 매칭 요청 - 채용공고에 맞는 후보자 검색
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchRequest {
        private String jdText;           // 채용공고 내용
        private List<String> requiredSkills;  // 필수 스킬 (optional)
        private Integer topK;            // 상위 몇 명 (default: 10)
    }

    /**
     * 스코어 요청 - 특정 후보자의 상세 점수 계산
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreRequest {
        private String candidateId;      // 후보자 ID (resumeId)
        private String jdText;           // 채용공고 내용
        private List<String> requiredSkills;  // 필수 스킬 (optional)
    }

    // ============================================
    // Response DTOs
    // ============================================

    /**
     * 매칭 결과 - 후보자 목록
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchResponse {
        private List<MatchedCandidate> matches;
        private int totalCount;
    }

    /**
     * 매칭된 후보자 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedCandidate {
        private String candidateId;
        private Double score;
        // 추가 정보 (DB에서 조회)
        private String name;
        private List<String> skills;
    }

    /**
     * 스코어 결과 - 상세 점수
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreResponse {
        private String candidateId;
        private List<String> requiredSkills;   // JD에서 추출된 스킬
        private List<String> candidateSkills;  // 후보자 스킬
        private Double cosineSimilarity;       // 벡터 유사도
        private ScoreDetail scoreDetail;       // 상세 점수
    }

    /**
     * 점수 상세
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDetail {
        private Double skillScore;       // 스킬 매칭 점수 (0~40)
        private Double similarityScore;  // 유사도 점수 (0~40)
        private Double bonusScore;       // 보너스 점수 (0~20)
        private Double totalScore;       // 총점 (0~100)
        private String grade;            // 등급 (A/B/C/D/F)
    }
}
