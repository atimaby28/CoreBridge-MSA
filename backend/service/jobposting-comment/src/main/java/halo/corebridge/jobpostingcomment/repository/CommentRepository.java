package halo.corebridge.jobpostingcomment.repository;

import halo.corebridge.jobpostingcomment.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query(
            value = "select count(*) from (" +
                    "   select comment_id from comment " +
                    "   where jobposting_id = :jobpostingId and parent_comment_id = :parentCommentId " +
                    "   limit :limit" +
                    ") t",
            nativeQuery = true
    )
    Long countBy(
            @Param("jobpostingId") Long jobpostingId,
            @Param("parentCommentId") Long parentCommentId,
            @Param("limit") Long limit
    );

    @Query(
            value = """
        select comment.comment_id, comment.content, comment.parent_comment_id,
               comment.jobposting_id, comment.user_id, comment.deleted,
               comment.created_at, comment.updated_at
        from (
            select comment_id
            from comment
            where jobposting_id = :jobpostingId
            order by parent_comment_id asc, comment_id asc
            limit :limit offset :offset
        ) t left join comment on t.comment_id = comment.comment_id
        """,
            nativeQuery = true
    )
    List<Comment> findAll(
            @Param("jobpostingId") Long jobpostingId,
            @Param("offset") Long offset,
            @Param("limit") Long limit
    );


    @Query(
            value = "select count(*) from (" +
                    "   select comment_id from comment where jobposting_id = :jobpostingId limit :limit" +
                    ") t",
            nativeQuery = true
    )
    Long count(
            @Param("jobpostingId") Long jobpostingId,
            @Param("limit") Long limit
    );
}
