package halo.corebridge.jobpostinghot.model.dto;

import halo.corebridge.jobpostinghot.model.entity.HotJobposting;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class HotJobpostingDto {

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

        public static Response from(HotJobposting hotJobposting) {
            return Response.builder()
                    .jobpostingId(hotJobposting.getJobpostingId())
                    .title(hotJobposting.getTitle())
                    .boardId(hotJobposting.getBoardId())
                    .likeCount(hotJobposting.getLikeCount())
                    .commentCount(hotJobposting.getCommentCount())
                    .viewCount(hotJobposting.getViewCount())
                    .score(hotJobposting.getScore())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ListResponse {
        private List<Response> hotJobpostings;
        private Integer count;

        public static ListResponse of(List<Response> hotJobpostings) {
            return ListResponse.builder()
                    .hotJobpostings(hotJobpostings)
                    .count(hotJobpostings.size())
                    .build();
        }
    }
}
