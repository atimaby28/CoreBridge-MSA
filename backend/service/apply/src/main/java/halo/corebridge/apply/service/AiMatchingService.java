package halo.corebridge.apply.service;

import halo.corebridge.apply.client.AiMatchingClient;
import halo.corebridge.apply.client.ResumeClient;
import halo.corebridge.apply.client.UserClient;
import halo.corebridge.apply.model.dto.AiMatchingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMatchingService {

    private final AiMatchingClient aiMatchingClient;
    private final UserClient userClient;
    private final ResumeClient resumeClient;

    /**
     * 채용공고에 맞는 후보자 매칭 (회사용)
     * - FastAPI에서 매칭 결과를 받은 후
     * - candidateId(=userId)로 User 서비스에서 닉네임 조회
     * - resumeId가 없으면 Resume 서비스에서 fallback 조회
     */
    public AiMatchingDto.MatchCandidatesResponse matchCandidates(AiMatchingDto.MatchCandidatesRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 10;

        List<AiMatchingDto.MatchedCandidate> matches = aiMatchingClient.matchCandidates(
                request.getJdText(), topK
        );

        // candidateId(=userId)로 닉네임 + resumeId enrichment
        List<AiMatchingDto.MatchedCandidate> enriched = matches.stream()
                .map(m -> {
                    String lookupId = m.getUserId() != null ? m.getUserId() : m.getCandidateId();
                    String nickname = userClient.getNickname(lookupId);

                    // resumeId: Redis 메타데이터 우선, 없으면 Resume 서비스 fallback
                    String resolvedResumeId = m.getResumeId();
                    if (resolvedResumeId == null && nickname != null) {
                        // 닉네임 조회 성공 = 유효한 userId → Resume 서비스에서 resumeId 조회
                        resolvedResumeId = resumeClient.getResumeId(lookupId);
                    }

                    return AiMatchingDto.MatchedCandidate.builder()
                            .candidateId(m.getCandidateId())
                            .userId(lookupId)
                            .resumeId(resolvedResumeId)
                            .score(m.getScore())
                            .name(nickname)
                            .skills(m.getSkills())
                            .build();
                })
                .toList();

        return AiMatchingDto.MatchCandidatesResponse.builder()
                .matches(enriched)
                .totalCount(enriched.size())
                .build();
    }

    /**
     * 이력서에 맞는 채용공고 매칭 (구직자용)
     */
    public AiMatchingDto.MatchJobpostingsResponse matchJobpostings(AiMatchingDto.MatchJobpostingsRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 10;

        List<AiMatchingDto.MatchedJobposting> matches = aiMatchingClient.matchJobpostings(
                request.getResumeText(), topK
        );

        return AiMatchingDto.MatchJobpostingsResponse.builder()
                .matches(matches)
                .totalCount(matches.size())
                .build();
    }

    /**
     * 특정 후보자의 상세 스코어 계산 (회사용)
     */
    public AiMatchingDto.ScoreResponse scoreCandidate(AiMatchingDto.ScoreRequest request) {
        return aiMatchingClient.scoreCandidate(
                request.getCandidateId(),
                request.getJdText(),
                request.getRequiredSkills()
        );
    }

    /**
     * 스킬 갭 분석 (구직자용)
     */
    public AiMatchingDto.SkillGapResponse analyzeSkillGap(AiMatchingDto.SkillGapRequest request) {
        return aiMatchingClient.analyzeSkillGap(
                request.getCandidateId(),
                request.getJobpostingId()
        );
    }
}
