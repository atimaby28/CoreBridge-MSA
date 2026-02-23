package halo.corebridge.jobpostingview.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobposting_view_count")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobpostingViewCount extends BaseTimeEntity {

    @Id
    private Long jobpostingId;

    private Long viewCount;

    public static JobpostingViewCount init(Long jobpostingId, Long viewCount) {
        JobpostingViewCount entity = new JobpostingViewCount();
        entity.jobpostingId = jobpostingId;
        entity.viewCount = viewCount;
        return entity;
    }

}
