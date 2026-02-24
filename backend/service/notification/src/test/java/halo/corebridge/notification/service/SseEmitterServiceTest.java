package halo.corebridge.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseEmitterService 테스트")
class SseEmitterServiceTest {

    private SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService();
    }

    @Test
    @DisplayName("성공: SSE 구독하면 연결이 등록된다")
    void subscribe_registersConnection() {
        sseEmitterService.subscribe(100L);

        assertThat(sseEmitterService.isConnected(100L)).isTrue();
        assertThat(sseEmitterService.getConnectionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공: 같은 유저가 재구독하면 기존 연결이 교체된다")
    void subscribe_replacesExisting() {
        sseEmitterService.subscribe(100L);
        sseEmitterService.subscribe(100L);

        assertThat(sseEmitterService.getConnectionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공: 여러 유저가 동시에 구독 가능하다")
    void subscribe_multipleUsers() {
        sseEmitterService.subscribe(100L);
        sseEmitterService.subscribe(200L);
        sseEmitterService.subscribe(300L);

        assertThat(sseEmitterService.getConnectionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("성공: 연결되지 않은 유저는 isConnected가 false")
    void isConnected_notSubscribed_returnsFalse() {
        assertThat(sseEmitterService.isConnected(999L)).isFalse();
    }

    @Test
    @DisplayName("성공: 초기 상태에서 연결 수는 0이다")
    void getConnectionCount_initial_zero() {
        assertThat(sseEmitterService.getConnectionCount()).isZero();
    }
}
