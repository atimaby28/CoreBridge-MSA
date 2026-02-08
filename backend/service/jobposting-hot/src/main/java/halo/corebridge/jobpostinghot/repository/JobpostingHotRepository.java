package halo.corebridge.jobpostinghot.repository;

import halo.corebridge.jobpostinghot.model.entity.JobpostingHot;
import halo.corebridge.jobpostinghot.model.entity.JobpostingHotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JobpostingHotRepository extends JpaRepository<JobpostingHot, JobpostingHotId> {
    
    @Query("SELECT h FROM JobpostingHot h WHERE h.dateKey = :dateKey ORDER BY h.score DESC")
    List<JobpostingHot> findByDateKeyOrderByScoreDesc(@Param("dateKey") LocalDate dateKey);
    
    @Query("SELECT h FROM JobpostingHot h WHERE h.dateKey = :dateKey ORDER BY h.score DESC LIMIT :limit")
    List<JobpostingHot> findTopByDateKey(@Param("dateKey") LocalDate dateKey, @Param("limit") int limit);
}
