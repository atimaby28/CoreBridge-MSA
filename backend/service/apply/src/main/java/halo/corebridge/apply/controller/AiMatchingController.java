package halo.corebridge.apply.controller;

import halo.corebridge.apply.model.dto.AiMatchingDto;
import halo.corebridge.apply.service.AiMatchingService;
import halo.corebridge.common.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai-matching")
@RequiredArgsConstructor
public class AiMatchingController {

    private final AiMatchingService aiMatchingService;

    /**
     * JD에 맞는 후보자 매칭
     * POST /api/v1/ai-matching/match
     */
    @PostMapping("/match")
    public ResponseEntity<BaseResponse<AiMatchingDto.MatchResponse>> matchCandidates(
            @RequestBody AiMatchingDto.MatchRequest request) {

        log.info("AI 매칭 요청: topK={}", request.getTopK());
        AiMatchingDto.MatchResponse response = aiMatchingService.matchCandidates(request);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * 특정 후보자의 상세 스코어 계산
     * POST /api/v1/ai-matching/score
     */
    @PostMapping("/score")
    public ResponseEntity<BaseResponse<AiMatchingDto.ScoreResponse>> scoreCandidate(
            @RequestBody AiMatchingDto.ScoreRequest request) {

        log.info("스코어 계산 요청: candidateId={}", request.getCandidateId());
        AiMatchingDto.ScoreResponse response = aiMatchingService.scoreCandidate(request);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * 채용공고 ID로 후보자 매칭
     * GET /api/v1/ai-matching/jobposting/{jobpostingId}
     */
    @GetMapping("/jobposting/{jobpostingId}")
    public ResponseEntity<BaseResponse<AiMatchingDto.MatchResponse>> matchByJobposting(
            @PathVariable Long jobpostingId,
            @RequestParam(defaultValue = "10") int topK) {

        log.info("채용공고 기반 매칭 요청: jobpostingId={}, topK={}", jobpostingId, topK);
        AiMatchingDto.MatchResponse response = aiMatchingService.matchByJobpostingId(jobpostingId, topK);

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
