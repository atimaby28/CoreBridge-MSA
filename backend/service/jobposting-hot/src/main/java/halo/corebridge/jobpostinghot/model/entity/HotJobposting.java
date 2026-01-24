package halo.corebridge.jobpostinghot.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "hot_jobposting")
@IdClass(HotJobpostingId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotJobposting extends BaseTimeEntity {
    
    @Id
    private LocalDate dateKey;
    
    @Id
    private Long jobpostingId;
    
    private String title;
    
    private Long boardId;
    
    private Long likeCount;
    
    private Long commentCount;
    
    private Long viewCount;
    
    private Double score;

    public static HotJobposting create(
            LocalDate dateKey,
            Long jobpostingId,
            String title,
            Long boardId,
            Long likeCount,
            Long commentCount,
            Long viewCount
    ) {
        HotJobposting entity = new HotJobposting();
        entity.dateKey = dateKey;
        entity.jobpostingId = jobpostingId;
        entity.title = title;
        entity.boardId = boardId;
        entity.likeCount = likeCount;
        entity.commentCount = commentCount;
        entity.viewCount = viewCount;
        entity.score = calculateScore(likeCount, commentCount, viewCount);
        return entity;
    }
    
    public void updateCounts(Long likeCount, Long commentCount, Long viewCount) {
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
        this.score = calculateScore(likeCount, commentCount, viewCount);
    }

    private static Double calculateScore(Long likeCount, Long commentCount, Long viewCount) {
        // 점수 계산: 좋아요 * 3 + 댓글 * 2 + 조회수 * 1
        return likeCount * 3.0 + commentCount * 2.0 + viewCount * 1.0;
    }
}
