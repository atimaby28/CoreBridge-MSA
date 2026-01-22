package halo.corebridge.jobposting.model.dto;

import halo.corebridge.jobposting.model.entity.Jobposting;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class JobpostingDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobpostingCreateRequest {
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 2, max = 100, message = "제목은 2~100자 사이여야 합니다")
        private String title;

        @NotBlank(message = "내용은 필수입니다")
        private String content;

        @NotNull(message = "게시판 ID는 필수입니다")
        private Long boardId;

        // userId는 Controller에서 SecurityContext로부터 주입됨 (Request Body에서 받지 않음)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobpostingUpdateRequest {
        @Size(min = 2, max = 100, message = "제목은 2~100자 사이여야 합니다")
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

        public static JobpostingResponse from(Jobposting jobposting) {
            return JobpostingResponse.builder()
                    .jobpostingId(jobposting.getJobpostingId())
                    .title(jobposting.getTitle())
                    .content(jobposting.getContent())
                    .boardId(jobposting.getBoardId())
                    .userId(jobposting.getUserId())
                    .createdAt(jobposting.getCreatedAt())
                    .updatedAt(jobposting.getUpdatedAt())
                    .build();
        }
    }

    @Builder
    @Getter
    public static class JobpostingPageResponse {
        private List<JobpostingResponse> jobpostings;
        private Long jobpostingCount;

        public static JobpostingPageResponse of(List<JobpostingResponse> jobpostings, Long jobpostingCount) {
            return JobpostingPageResponse.builder()
                    .jobpostings(jobpostings)
                    .jobpostingCount(jobpostingCount)
                    .build();
        }
    }

    @Builder
    @Getter
    public static class JobpostingListResponse {
        private List<JobpostingResponse> jobpostings;

        public static JobpostingListResponse of(List<JobpostingResponse> jobpostings) {
            return JobpostingListResponse.builder()
                    .jobpostings(jobpostings)
                    .build();
        }
    }
}
