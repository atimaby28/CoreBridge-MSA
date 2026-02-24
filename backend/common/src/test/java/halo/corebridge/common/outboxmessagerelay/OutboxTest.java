package halo.corebridge.common.outboxmessagerelay;

import halo.corebridge.common.event.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Outbox 엔티티 테스트")
class OutboxTest {

    @Nested
    @DisplayName("create() - Outbox 생성")
    class CreateTest {

        @Test
        @DisplayName("성공: Outbox가 올바르게 생성된다")
        void create_success() {
            // when
            Outbox outbox = Outbox.create(EventType.JOBPOSTING_CREATED, "{\"test\":true}", 2L);

            // then
            assertThat(outbox.getEventType()).isEqualTo(EventType.JOBPOSTING_CREATED);
            assertThat(outbox.getPayload()).isEqualTo("{\"test\":true}");
            assertThat(outbox.getShardKey()).isEqualTo(2L);
            assertThat(outbox.getRetryCount()).isZero();
            assertThat(outbox.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("retry 관련 메서드")
    class RetryTest {

        @Test
        @DisplayName("성공: retryCount를 증가시킨다")
        void incrementRetryCount_increases() {
            // given
            Outbox outbox = Outbox.create(EventType.JOBPOSTING_CREATED, "{}", 0L);

            // when
            outbox.incrementRetryCount();

            // then
            assertThat(outbox.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: MAX_RETRY_COUNT 미만이면 재시도 가능하다")
        void isRetryExhausted_belowMax_returnsFalse() {
            // given
            Outbox outbox = Outbox.create(EventType.JOBPOSTING_CREATED, "{}", 0L);

            // when & then
            assertThat(outbox.isRetryExhausted()).isFalse();
        }

        @Test
        @DisplayName("성공: MAX_RETRY_COUNT에 도달하면 재시도 불가하다")
        void isRetryExhausted_atMax_returnsTrue() {
            // given
            Outbox outbox = Outbox.create(EventType.JOBPOSTING_CREATED, "{}", 0L);
            for (int i = 0; i < MessageRelayConstants.MAX_RETRY_COUNT; i++) {
                outbox.incrementRetryCount();
            }

            // when & then
            assertThat(outbox.isRetryExhausted()).isTrue();
        }
    }
}
