package halo.corebridge.resume.repository;

import halo.corebridge.resume.model.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
}
