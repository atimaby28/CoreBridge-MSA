package halo.corebridge.apply.service;

import halo.corebridge.apply.client.AiMatchingClient;
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

    /**
     * 채용공고에 맞는 후보자 매칭 (회사용)
     */
    public AiMatchingDto.MatchCandidatesResponse matchCandidates(AiMatchingDto.MatchCandidatesRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 10;

        List<AiMatchingDto.MatchedCandidate> matches = aiMatchingClient.matchCandidates(
                request.getJdText(), topK
        );

        return AiMatchingDto.MatchCandidatesResponse.builder()
                .matches(matches)
                .totalCount(matches.size())
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
