package halo.corebridge.schedule.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleType {

    CODING_TEST("코딩 테스트"),
    INTERVIEW_1("1차 면접"),
    INTERVIEW_2("2차 면접"),
    FINAL_INTERVIEW("최종 면접"),
    ORIENTATION("오리엔테이션"),
    OTHER("기타");

    private final String description;
}
