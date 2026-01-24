package halo.corebridge.jobpostinghot.repository;

import halo.corebridge.jobpostinghot.model.entity.HotJobposting;
import halo.corebridge.jobpostinghot.model.entity.HotJobpostingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HotJobpostingRepository extends JpaRepository<HotJobposting, HotJobpostingId> {
    
    @Query("SELECT h FROM HotJobposting h WHERE h.dateKey = :dateKey ORDER BY h.score DESC")
    List<HotJobposting> findByDateKeyOrderByScoreDesc(@Param("dateKey") LocalDate dateKey);
    
    @Query("SELECT h FROM HotJobposting h WHERE h.dateKey = :dateKey ORDER BY h.score DESC LIMIT :limit")
    List<HotJobposting> findTopByDateKey(@Param("dateKey") LocalDate dateKey, @Param("limit") int limit);
}
