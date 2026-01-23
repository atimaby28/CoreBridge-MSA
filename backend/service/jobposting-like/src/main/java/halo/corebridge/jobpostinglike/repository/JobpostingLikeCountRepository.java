package halo.corebridge.jobpostinglike.repository;

import halo.corebridge.jobpostinglike.entity.JobpostingLikeCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobpostingLikeCountRepository extends JpaRepository<JobpostingLikeCount, Long> {

    // UPDATE 쿼리로 좋아요 수 증가
    @Modifying
    @Query("UPDATE JobpostingLikeCount l SET l.likeCount = l.likeCount + 1 WHERE l.jobpostingId = :jobpostingId")
    int increase(@Param("jobpostingId") Long jobpostingId);

    // UPDATE 쿼리로 좋아요 수 감소
    @Modifying
    @Query("UPDATE JobpostingLikeCount l SET l.likeCount = l.likeCount - 1 WHERE l.jobpostingId = :jobpostingId")
    int decrease(@Param("jobpostingId") Long jobpostingId);

}
