package halo.corebridge.resume.repository;

import halo.corebridge.resume.model.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
    
    /**
     * 이력서 ID로 모든 버전 조회 (최신순)
     */
    List<ResumeVersion> findByResumeIdOrderByVersionDesc(Long resumeId);
    
    /**
     * 특정 버전 조회
     */
    Optional<ResumeVersion> findByResumeIdAndVersion(Long resumeId, int version);
    
    /**
     * 이력서의 최신 버전 번호 조회
     */
    Optional<ResumeVersion> findTopByResumeIdOrderByVersionDesc(Long resumeId);
}
