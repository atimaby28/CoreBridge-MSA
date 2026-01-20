package halo.corebridge.common.audit.annotation;

import halo.corebridge.common.audit.enums.AuditEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 레벨 감사 로깅 어노테이션
 * 특정 메서드에 명시적으로 감사 로그를 남기고 싶을 때 사용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * 감사 이벤트 타입
     */
    AuditEventType eventType() default AuditEventType.API_REQUEST;

    /**
     * 추가 설명
     */
    String description() default "";

    /**
     * 요청 바디 로깅 여부
     */
    boolean logRequestBody() default true;
}
