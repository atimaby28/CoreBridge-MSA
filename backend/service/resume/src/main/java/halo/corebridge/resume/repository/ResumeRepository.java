package halo.corebridge.resume.repository;

import halo.corebridge.resume.model.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    
    /**
     * 사용자 ID로 이력서 조회 (사용자당 1개)
     */
    Optional<Resume> findByUserId(Long userId);
    
    /**
     * 사용자 ID로 이력서 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}
