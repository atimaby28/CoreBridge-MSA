package halo.corebridge.common.audit.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 이벤트 타입
 */
@Getter
@RequiredArgsConstructor
public enum AuditEventType {

    // ========== 인증 ==========
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    LOGIN_FAILED("로그인 실패"),

    // ========== 사용자 ==========
    USER_CREATE("회원가입"),
    USER_UPDATE("회원정보 수정"),
    USER_DELETE("회원탈퇴"),

    // ========== 채용공고 ==========
    JOBPOSTING_CREATE("공고 등록"),
    JOBPOSTING_UPDATE("공고 수정"),
    JOBPOSTING_DELETE("공고 삭제"),
    JOBPOSTING_READ("공고 조회"),

    // ========== 지원 ==========
    APPLICATION_CREATE("지원"),
    APPLICATION_CANCEL("지원 취소"),
    APPLICATION_STATUS_CHANGE("지원 상태 변경"),

    // ========== 이력서 ==========
    RESUME_CREATE("이력서 등록"),
    RESUME_UPDATE("이력서 수정"),
    RESUME_DELETE("이력서 삭제"),

    // ========== 면접 ==========
    SCHEDULE_CREATE("일정 등록"),
    SCHEDULE_UPDATE("일정 수정"),
    SCHEDULE_CANCEL("일정 취소"),

    // ========== 알림 ==========
    NOTIFICATION_READ("알림 확인"),

    // ========== 일반 ==========
    API_REQUEST("API 요청"),

    // ========== 시스템 ==========
    SYSTEM_ERROR("시스템 오류"),
    UNKNOWN("알 수 없음");

    private final String displayName;
}
