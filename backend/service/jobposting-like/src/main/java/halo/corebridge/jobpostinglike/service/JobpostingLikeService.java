package halo.corebridge.jobpostinglike.service;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.jobpostinglike.dto.JobpostingLikeResponse;
import halo.corebridge.jobpostinglike.entity.JobpostingLike;
import halo.corebridge.jobpostinglike.entity.JobpostingLikeCount;
import halo.corebridge.jobpostinglike.repository.JobpostingLikeCountRepository;
import halo.corebridge.jobpostinglike.repository.JobpostingLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobpostingLikeService {

    private final Snowflake snowflake = new Snowflake();
    private final JobpostingLikeRepository jobpostingLikeRepository;
    private final JobpostingLikeCountRepository jobpostingLikeCountRepository;

    /**
     * 좋아요 상태 조회
     */
    @Transactional(readOnly = true)
    public JobpostingLikeResponse read(Long jobpostingId, Long userId) {
        return jobpostingLikeRepository.findByJobpostingIdAndUserId(jobpostingId, userId)
                .map(like -> JobpostingLikeResponse.from(like, true))
                .orElse(JobpostingLikeResponse.notLiked(jobpostingId, userId));
    }

    /**
     * 좋아요 수 조회
     */
    @Transactional(readOnly = true)
    public Long count(Long jobpostingId) {
        return jobpostingLikeCountRepository.findById(jobpostingId)
                .map(JobpostingLikeCount::getLikeCount)
                .orElse(0L);
    }

    /**
     * 좋아요 - (UPDATE 쿼리 - 증가)
     */
    @Transactional
    public void like(Long jobpostingId, Long userId) {
        // 이미 좋아요한 경우 무시
        if (jobpostingLikeRepository.existsByJobpostingIdAndUserId(jobpostingId, userId)) {
            return;
        }

        jobpostingLikeRepository.save(
                JobpostingLike.create(snowflake.nextId(), jobpostingId, userId)
        );

        int result = jobpostingLikeCountRepository.increase(jobpostingId);
        if (result == 0) {
            jobpostingLikeCountRepository.save(
                    JobpostingLikeCount.init(jobpostingId, 1L)
            );
        }
    }

    /**
     * 좋아요 취소 - (UPDATE 쿼리 - 감소)
     */
    @Transactional
    public void unlike(Long jobpostingId, Long userId) {
        jobpostingLikeRepository.findByJobpostingIdAndUserId(jobpostingId, userId)
                .ifPresent(like -> {
                    jobpostingLikeRepository.delete(like);
                    jobpostingLikeCountRepository.decrease(jobpostingId);
                });
    }
}
