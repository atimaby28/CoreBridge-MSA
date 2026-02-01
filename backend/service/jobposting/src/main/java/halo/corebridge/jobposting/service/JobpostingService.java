package halo.corebridge.jobposting.service;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import halo.corebridge.jobposting.model.entity.Jobposting;
import halo.corebridge.jobposting.repository.JobpostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobpostingService {

    private final Snowflake snowflake = new Snowflake();
    private final JobpostingRepository jobpostingRepository;

    // ============================================
    // 생성
    // ============================================

    /**
     * 채용공고 생성
     * @param userId 인증된 사용자 ID (SecurityContext에서 추출)
     * @param request 생성 요청
     */
    @Transactional
    public JobpostingDto.JobpostingResponse create(Long userId, JobpostingDto.JobpostingCreateRequest request) {
        Jobposting jobposting = jobpostingRepository.save(
                Jobposting.create(
                        snowflake.nextId(),
                        request.getTitle(),
                        request.getContent(),
                        request.getBoardId(),
                        userId,
                        toJson(request.getRequiredSkills()),
                        toJson(request.getPreferredSkills())
                )
        );

        log.info("채용공고 생성: jobpostingId={}, userId={}", jobposting.getJobpostingId(), userId);

        return JobpostingDto.JobpostingResponse.from(jobposting);
    }

    // ============================================
    // 수정
    // ============================================

    /**
     * 채용공고 수정
     * @param userId 인증된 사용자 ID
     * @param jobpostingId 수정할 채용공고 ID
     * @param request 수정 요청
     */
    @Transactional
    public JobpostingDto.JobpostingResponse update(Long userId, Long jobpostingId, JobpostingDto.JobpostingUpdateRequest request) {
        Jobposting jobposting = jobpostingRepository.findById(jobpostingId)
                .orElseThrow(() -> new IllegalArgumentException("채용공고를 찾을 수 없습니다: " + jobpostingId));

        // 본인 검증
        validateOwner(jobposting, userId);

        jobposting.update(
                request.getTitle(),
                request.getContent(),
                toJson(request.getRequiredSkills()),
                toJson(request.getPreferredSkills())
        );

        log.info("채용공고 수정: jobpostingId={}, userId={}", jobpostingId, userId);

        return JobpostingDto.JobpostingResponse.from(jobposting);
    }

    // ============================================
    // 삭제
    // ============================================

    /**
     * 채용공고 삭제
     * @param userId 인증된 사용자 ID
     * @param jobpostingId 삭제할 채용공고 ID
     */
    @Transactional
    public void delete(Long userId, Long jobpostingId) {
        Jobposting jobposting = jobpostingRepository.findById(jobpostingId)
                .orElseThrow(() -> new IllegalArgumentException("채용공고를 찾을 수 없습니다: " + jobpostingId));

        // 본인 검증
        validateOwner(jobposting, userId);

        jobpostingRepository.deleteById(jobpostingId);

        log.info("채용공고 삭제: jobpostingId={}, userId={}", jobpostingId, userId);
    }

    // ============================================
    // 조회
    // ============================================

    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public JobpostingDto.JobpostingResponse read(Long jobpostingId) {
        return JobpostingDto.JobpostingResponse.from(
                jobpostingRepository.findById(jobpostingId)
                        .orElseThrow(() -> new IllegalArgumentException("채용공고를 찾을 수 없습니다: " + jobpostingId))
        );
    }

    /**
     * 게시판별 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public JobpostingDto.JobpostingPageResponse readAll(Long boardId, Long page, Long pageSize) {
        long offset = (page - 1) * pageSize;
        long limit = PageLimitCalculator.calculatePageLimit(page, pageSize, 10L);

        // boardId=1은 "전체" 게시판 → 모든 공고 조회
        if (boardId == 1L) {
            return JobpostingDto.JobpostingPageResponse.of(
                    jobpostingRepository.findAllBoards(offset, pageSize).stream()
                            .map(JobpostingDto.JobpostingResponse::from)
                            .toList(),
                    jobpostingRepository.countAll(limit)
            );
        }

        return JobpostingDto.JobpostingPageResponse.of(
                jobpostingRepository.findAll(boardId, offset, pageSize).stream()
                        .map(JobpostingDto.JobpostingResponse::from)
                        .toList(),
                jobpostingRepository.count(boardId, limit)
        );
    }

    /**
     * 작성자별 목록 조회
     */
    @Transactional(readOnly = true)
    public JobpostingDto.JobpostingListResponse readByWriter(Long writerId) {
        return JobpostingDto.JobpostingListResponse.of(
                jobpostingRepository.findByUserIdOrderByCreatedAtDesc(writerId).stream()
                        .map(JobpostingDto.JobpostingResponse::from)
                        .toList()
        );
    }

    // ============================================
    // Private Methods
    // ============================================

    /**
     * 본인 검증
     * 채용공고 작성자와 요청자가 동일한지 확인
     */
    private void validateOwner(Jobposting jobposting, Long userId) {
        if (!jobposting.getUserId().equals(userId)) {
            throw new IllegalStateException("본인이 작성한 채용공고만 수정/삭제할 수 있습니다");
        }
    }

    /**
     * 스킬 리스트를 JSON 배열 문자열로 변환
     * ["Java", "Spring"] -> "[\"Java\",\"Spring\"]"
     */
    private String toJson(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        return "[\"" + String.join("\",\"", skills) + "\"]";
    }
}
