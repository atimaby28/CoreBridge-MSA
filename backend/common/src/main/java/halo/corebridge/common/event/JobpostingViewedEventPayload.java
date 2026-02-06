package halo.corebridge.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobpostingViewedEventPayload implements EventPayload {
    private Long jobpostingId;
    private Long viewCount;
}
