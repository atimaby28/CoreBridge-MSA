package halo.corebridge.jobpostinglike.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "jobposting_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"jobpostingId", "userId"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobpostingLike extends BaseTimeEntity {

    @Id
    private Long jobpostingLikeId;

    private Long jobpostingId;

    private Long userId;

    public static JobpostingLike create(Long jobpostingLikeId, Long jobpostingId, Long userId) {
        JobpostingLike entity = new JobpostingLike();
        entity.jobpostingLikeId = jobpostingLikeId;
        entity.jobpostingId = jobpostingId;
        entity.userId = userId;
        return entity;
    }
}
