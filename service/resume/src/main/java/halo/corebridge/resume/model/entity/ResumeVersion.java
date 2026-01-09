package halo.corebridge.resume.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;
    private int version;
    private String fileUrl;
    private String fileType;

    @Lob
    private String extractedText;

    public ResumeVersion(Long resumeId, int version, String fileUrl, String fileType) {
        this.resumeId = resumeId;
        this.version = version;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }
}
