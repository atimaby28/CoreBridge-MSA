package halo.corebridge.jobpostinglike.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobposting_like_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobpostingLikeCount extends BaseTimeEntity {

    @Id
    private Long jobpostingId;

    private Long likeCount;

    public static JobpostingLikeCount init(Long jobpostingId, Long likeCount) {
        JobpostingLikeCount entity = new JobpostingLikeCount();
        entity.jobpostingId = jobpostingId;
        entity.likeCount = likeCount;
        return entity;
    }
}
