package halo.corebridge.common.snowflake;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Snowflake ID 생성기 테스트")
class SnowflakeTest {

    @Test
    @DisplayName("성공: ID가 양수이고 0이 아니다")
    void nextId_returnsPositiveId() {
        Snowflake snowflake = new Snowflake(1);
        long id = snowflake.nextId();
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("성공: 연속 생성된 ID는 모두 유니크하다")
    void nextId_generatesUniqueIds() {
        Snowflake snowflake = new Snowflake(1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(snowflake.nextId());
        }
        assertThat(ids).hasSize(1000);
    }

    @Test
    @DisplayName("성공: 서로 다른 노드에서 생성된 ID도 유니크하다")
    void nextId_differentNodes_uniqueIds() {
        Snowflake node1 = new Snowflake(1);
        Snowflake node2 = new Snowflake(2);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(node1.nextId());
            ids.add(node2.nextId());
        }
        assertThat(ids).hasSize(200);
    }

    @Test
    @DisplayName("성공: ID는 시간순으로 증가한다")
    void nextId_idsAreMonotonicallyIncreasing() {
        Snowflake snowflake = new Snowflake(1);
        long prev = snowflake.nextId();
        for (int i = 0; i < 100; i++) {
            long current = snowflake.nextId();
            assertThat(current).isGreaterThan(prev);
            prev = current;
        }
    }

    @Test
    @DisplayName("성공: 기본 생성자로도 ID를 생성할 수 있다")
    void defaultConstructor_works() {
        Snowflake snowflake = new Snowflake();
        long id = snowflake.nextId();
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("실패: nodeId가 음수이면 예외 발생")
    void constructor_negativeNodeId_throws() {
        assertThatThrownBy(() -> new Snowflake(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실패: nodeId가 최대값을 초과하면 예외 발생")
    void constructor_nodeIdExceedsMax_throws() {
        assertThatThrownBy(() -> new Snowflake(1024))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
