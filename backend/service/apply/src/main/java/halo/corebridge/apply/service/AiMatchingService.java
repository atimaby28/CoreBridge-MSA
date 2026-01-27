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
     * 채용공고에 맞는 후보자 매칭
     */
    public AiMatchingDto.MatchResponse matchCandidates(AiMatchingDto.MatchRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 10;

        List<AiMatchingDto.MatchedCandidate> matches = aiMatchingClient.matchCandidates(
                request.getJdText(),
                topK
        );

        return AiMatchingDto.MatchResponse.builder()
                .matches(matches)
                .totalCount(matches.size())
                .build();
    }

    /**
     * 특정 후보자의 상세 스코어 계산
     */
    public AiMatchingDto.ScoreResponse scoreCandidate(AiMatchingDto.ScoreRequest request) {
        return aiMatchingClient.scoreCandidate(
                request.getCandidateId(),
                request.getJdText(),
                request.getRequiredSkills()
        );
    }

    /**
     * 채용공고 ID로 후보자 매칭 (Jobposting 서비스와 연동 필요)
     */
    public AiMatchingDto.MatchResponse matchByJobpostingId(Long jobpostingId, int topK) {
        // TODO: Jobposting 서비스에서 JD 내용 조회
        // 현재는 직접 JD 텍스트를 전달받는 방식 사용
        log.warn("matchByJobpostingId는 아직 구현되지 않았습니다. matchCandidates를 사용해주세요.");
        return AiMatchingDto.MatchResponse.builder()
                .matches(List.of())
                .totalCount(0)
                .build();
    }
}
