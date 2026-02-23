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

    // ============================================
    // 회사용: 후보자 매칭
    // ============================================

    /**
     * 채용공고 기반 후보자 매칭
     * POST /api/v1/ai-matching/match
     */
    @PostMapping("/match")
    public ResponseEntity<BaseResponse<AiMatchingDto.MatchCandidatesResponse>> matchCandidates(
            @RequestBody AiMatchingDto.MatchCandidatesRequest request) {

        log.info("AI 후보자 매칭 요청: topK={}", request.getTopK());
        AiMatchingDto.MatchCandidatesResponse response = aiMatchingService.matchCandidates(request);
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
            log.error("스코어 계산 결과 null: candidateId={}", request.getCandidateId());
            return ResponseEntity.internalServerError()
                    .body((BaseResponse) BaseResponse.failure(5000, "스코어 계산에 실패했습니다. AI 서비스 로그를 확인해주세요."));
        }
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // ============================================
    // 구직자용: 채용공고 추천
    // ============================================

    /**
     * 이력서 기반 채용공고 추천
     * POST /api/v1/ai-matching/match-jobpostings
     */
    @PostMapping("/match-jobpostings")
    public ResponseEntity<BaseResponse<AiMatchingDto.MatchJobpostingsResponse>> matchJobpostings(
            @RequestBody AiMatchingDto.MatchJobpostingsRequest request) {

        log.info("AI 채용공고 추천 요청: topK={}", request.getTopK());
        AiMatchingDto.MatchJobpostingsResponse response = aiMatchingService.matchJobpostings(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * 스킬 갭 분석
     * POST /api/v1/ai-matching/skill-gap
     */
    @PostMapping("/skill-gap")
    public ResponseEntity<BaseResponse<AiMatchingDto.SkillGapResponse>> analyzeSkillGap(
            @RequestBody AiMatchingDto.SkillGapRequest request) {

        log.info("스킬 갭 분석 요청: candidateId={}, jobpostingId={}", request.getCandidateId(), request.getJobpostingId());
        AiMatchingDto.SkillGapResponse response = aiMatchingService.analyzeSkillGap(request);

        if (response == null) {
            log.error("스킬 갭 분석 결과 null: candidateId={}, jobpostingId={}", request.getCandidateId(), request.getJobpostingId());
            return ResponseEntity.internalServerError()
                    .body((BaseResponse) BaseResponse.failure(5000, "스킬 갭 분석에 실패했습니다. AI 서비스 로그를 확인해주세요."));
        }
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
