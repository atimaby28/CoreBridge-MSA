package halo.corebridge.jobpostinglike.dto;

import halo.corebridge.jobpostinglike.entity.JobpostingLike;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobpostingLikeResponse {
    private Long jobpostingLikeId;
    private Long jobpostingId;
    private Long userId;
    private boolean liked;

    public static JobpostingLikeResponse from(JobpostingLike like, boolean liked) {
        return JobpostingLikeResponse.builder()
                .jobpostingLikeId(like.getJobpostingLikeId())
                .jobpostingId(like.getJobpostingId())
                .userId(like.getUserId())
                .liked(liked)
                .build();
    }

    public static JobpostingLikeResponse notLiked(Long jobpostingId, Long userId) {
        return JobpostingLikeResponse.builder()
                .jobpostingId(jobpostingId)
                .userId(userId)
                .liked(false)
                .build();
    }
}
