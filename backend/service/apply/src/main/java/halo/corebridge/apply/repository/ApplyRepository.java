package halo.corebridge.apply.repository;

import halo.corebridge.apply.model.entity.Apply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplyRepository extends JpaRepository<Apply, Long> {

    // 중복 지원 체크
    boolean existsByJobpostingIdAndUserId(Long jobpostingId, Long userId);

    // 특정 공고의 지원 목록 (기업용)
    List<Apply> findByJobpostingIdOrderByCreatedAtDesc(Long jobpostingId);

    // 특정 공고의 지원 수
    Long countByJobpostingId(Long jobpostingId);

    // 특정 사용자의 지원 목록 (구직자용)
    List<Apply> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 사용자의 지원 수
    Long countByUserId(Long userId);

    // 특정 공고 + 특정 사용자의 지원 조회
    Optional<Apply> findByJobpostingIdAndUserId(Long jobpostingId, Long userId);

    // 여러 공고의 총 지원 수 (기업 전체 통계용)
    @Query("SELECT COUNT(a) FROM Apply a WHERE a.jobpostingId IN :jobpostingIds")
    Long countByJobpostingIdIn(@Param("jobpostingIds") List<Long> jobpostingIds);
}
