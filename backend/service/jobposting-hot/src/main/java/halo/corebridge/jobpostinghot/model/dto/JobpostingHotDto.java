package halo.corebridge.jobpostinghot.model.dto;

import halo.corebridge.jobpostinghot.model.entity.JobpostingHot;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class JobpostingHotDto {

    @Getter
    @Builder
    public static class Response {
        private Long jobpostingId;
        private String title;
        private Long boardId;
        private Long likeCount;
        private Long commentCount;
        private Long viewCount;
        private Double score;

        public static Response from(JobpostingHot jobpostingHot) {
            return Response.builder()
                    .jobpostingId(jobpostingHot.getJobpostingId())
                    .title(jobpostingHot.getTitle())
                    .boardId(jobpostingHot.getBoardId())
                    .likeCount(jobpostingHot.getLikeCount())
                    .commentCount(jobpostingHot.getCommentCount())
                    .viewCount(jobpostingHot.getViewCount())
                    .score(jobpostingHot.getScore())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ListResponse {
        private List<Response> jobpostingHots;
        private Integer count;

        public static ListResponse of(List<Response> jobpostingHots) {
            return ListResponse.builder()
                    .jobpostingHots(jobpostingHots)
                    .count(jobpostingHots.size())
                    .build();
        }
    }
}
