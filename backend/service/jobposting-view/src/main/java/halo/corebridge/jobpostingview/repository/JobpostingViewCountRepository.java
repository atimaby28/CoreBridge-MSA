package halo.corebridge.jobpostingview.repository;

import halo.corebridge.jobpostingview.entity.JobpostingViewCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobpostingViewCountRepository extends JpaRepository<JobpostingViewCount, Long> {

    // 단순 update 쿼리로 조회수 증가 (비관적 락 없이)
    @Modifying
    @Query("UPDATE JobpostingViewCount v SET v.viewCount = v.viewCount + 1 WHERE v.jobpostingId = :jobpostingId")
    int increase(@Param("jobpostingId") Long jobpostingId);

}
