package halo.corebridge.jobpostinglike.repository;

import halo.corebridge.jobpostinglike.entity.JobpostingLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobpostingLikeRepository extends JpaRepository<JobpostingLike, Long> {

    Optional<JobpostingLike> findByJobpostingIdAndUserId(Long jobpostingId, Long userId);

    boolean existsByJobpostingIdAndUserId(Long jobpostingId, Long userId);
}
