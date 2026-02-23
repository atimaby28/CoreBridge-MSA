package halo.corebridge.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobpostingUpdatedEventPayload implements EventPayload {
    private Long jobpostingId;
    private String title;
    private String content;
    private Long boardId;
    private Long userId;
    private String requiredSkills;
    private String preferredSkills;
    private LocalDateTime updatedAt;
}
