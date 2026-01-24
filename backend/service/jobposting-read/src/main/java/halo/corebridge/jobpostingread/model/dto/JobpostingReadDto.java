package halo.corebridge.jobpostingread.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class JobpostingReadDto {

    @Getter
    @Builder
    public static class Response {
        private Long jobpostingId;
        private String title;
        private String content;
        private Long boardId;
        private Long userId;
        private String nickname;
        private Long viewCount;
        private Long likeCount;
        private Long commentCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class PageResponse {
        private List<Response> jobpostings;
        private Long jobpostingCount;

        public static PageResponse of(List<Response> jobpostings, Long jobpostingCount) {
            return PageResponse.builder()
                    .jobpostings(jobpostings)
                    .jobpostingCount(jobpostingCount)
                    .build();
        }
    }
}
