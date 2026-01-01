package halo.corebridge.jobposting.model.dto;

import halo.corebridge.jobposting.model.entity.Jobposting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class JobpostingDto {
    @Builder
    @Getter
    public static class JobpostingCreateRequest {
        private String title;
        private String content;
        private Long userId;
        private Long boardId;
    }

    @Builder
    @Getter
    public static class JobpostingUpdateRequest {
        private String title;
        private String content;
    }

    @Builder
    @Getter
    public static class JobpostingResponse {
        private Long jobpostingId;
        private String title;
        private String content;
        private Long boardId;
        private Long userId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static JobpostingDto.JobpostingResponse from(Jobposting jobposting) {
            return JobpostingResponse.builder()
                    .jobpostingId(jobposting.getJobpostingId())
                    .title(jobposting.getTitle())
                    .content(jobposting.getContent())
                    .boardId(jobposting.getBoardId())
                    .userId(jobposting.getWriterId())
                    .createdAt(jobposting.getCreatedAt())
                    .updatedAt(jobposting.getUpdatedAt())
                    .build();
        }
    }
}
