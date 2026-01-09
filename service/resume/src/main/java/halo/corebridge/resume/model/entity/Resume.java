package halo.corebridge.resume.model.entity;

import halo.corebridge.resume.model.enums.ResumeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String title;

    @Enumerated(EnumType.STRING)
    private ResumeStatus status;

    private int currentVersion;

    public Resume(Long userId, String title) {
        this.userId = userId;
        this.title = title;
        this.status = ResumeStatus.DRAFT;
        this.currentVersion = 0;
    }

    public void uploadNewVersion() {
        this.currentVersion++;
        this.status = ResumeStatus.UPLOADED;
    }

    public void markAnalyzing() {
        this.status = ResumeStatus.ANALYZING;
    }

    public void markAnalyzed() {
        this.status = ResumeStatus.ANALYZED;
    }
}