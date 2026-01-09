package halo.corebridge.resume.repository;


import halo.corebridge.resume.model.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    List<ResumeVersion> findByResumeId(Long resumeId);
}
