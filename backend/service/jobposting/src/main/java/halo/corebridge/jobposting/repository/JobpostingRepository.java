package halo.corebridge.jobposting.repository;

import halo.corebridge.jobposting.model.entity.Jobposting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobpostingRepository extends JpaRepository<Jobposting, Long> {
    @Query(
            value = "SELECT jobposting.jobposting_id, jobposting.title, jobposting.content, " +
                    "jobposting.board_id, jobposting.user_id, " +
                    "jobposting.required_skills, jobposting.preferred_skills, " +
                    "jobposting.created_at, jobposting.updated_at " +
                    "FROM (" +
                    "       SELECT jobposting_id FROM jobposting " +
                    "       WHERE board_id = :boardId " +
                    "       ORDER BY jobposting_id DESC" +
                    "       LIMIT :limit OFFSET :offset" +
                    ") t LEFT JOIN jobposting on t.jobposting_id = jobposting.jobposting_id",
            nativeQuery = true
    )
    List<Jobposting> findAll(@Param("boardId") Long boardId,
                             @Param("offset") Long offset,
                             @Param("limit") Long limit
    );

    @Query(
            value = "SELECT jobposting.jobposting_id, jobposting.title, jobposting.content, " +
                    "jobposting.board_id, jobposting.user_id, " +
                    "jobposting.required_skills, jobposting.preferred_skills, " +
                    "jobposting.created_at, jobposting.updated_at " +
                    "FROM (" +
                    "       SELECT jobposting_id FROM jobposting " +
                    "       ORDER BY jobposting_id DESC" +
                    "       LIMIT :limit OFFSET :offset" +
                    ") t LEFT JOIN jobposting on t.jobposting_id = jobposting.jobposting_id",
            nativeQuery = true
    )
    List<Jobposting> findAllBoards(@Param("offset") Long offset,
                                   @Param("limit") Long limit
    );

    @Query(
            value = "SELECT count(*) from (" +
                    "   SELECT jobposting_id FROM jobposting WHERE board_id = :boardId" +
                    "   LIMIT :limit" +
                    ") t",
            nativeQuery = true
    )
    Long count(@Param("boardId") Long boardId, @Param("limit") Long limit);

    @Query(
            value = "SELECT count(*) from (" +
                    "   SELECT jobposting_id FROM jobposting" +
                    "   LIMIT :limit" +
                    ") t",
            nativeQuery = true
    )
    Long countAll(@Param("limit") Long limit);

    List<Jobposting> findByUserIdOrderByCreatedAtDesc(Long userId);
}