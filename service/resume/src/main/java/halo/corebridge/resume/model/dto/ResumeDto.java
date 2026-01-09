package halo.corebridge.resume.model.dto;

import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.model.enums.ResumeStatus;
import lombok.Builder;
import lombok.Getter;

public class ResumeDto {
    @Getter
    public static class ResumeCreateRequest {
        private Long userId;
        private String title;
    }

    @Getter
    public static class ResumeUploadRequest {
        private String fileUrl;
        private String fileType;
    }

    @Getter
    @Builder
    public class ResumeResponse {

        private Long resumeId;
        private Long userId;
        private String title;
        private ResumeStatus status;
        private int currentVersion;

        public static ResumeResponse from(Resume resume) {
            return ResumeResponse.builder()
                    .resumeId(resume.getId())
                    .userId(resume.getUserId())
                    .title(resume.getTitle())
                    .status(resume.getStatus())
                    .currentVersion(resume.getCurrentVersion())
                    .build();
        }
    }
}
