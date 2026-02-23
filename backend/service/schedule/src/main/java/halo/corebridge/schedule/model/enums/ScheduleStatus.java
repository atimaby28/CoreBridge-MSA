package halo.corebridge.schedule.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleStatus {

    SCHEDULED("예정"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    CANCELLED("취소"),
    NO_SHOW("불참");

    private final String description;
}
