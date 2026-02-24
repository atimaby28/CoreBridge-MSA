package halo.corebridge.common.dataserializer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataSerializer 테스트")
class DataSerializerTest {

    @Nested
    @DisplayName("serialize() - 객체 직렬화")
    class SerializeTest {

        @Test
        @DisplayName("성공: 객체를 JSON 문자열로 직렬화한다")
        void serialize_validObject_returnsJson() {
            // given
            TestDto dto = new TestDto("hello", 42);

            // when
            String json = DataSerializer.serialize(dto);

            // then
            assertThat(json).isNotNull();
            assertThat(json).contains("\"name\":\"hello\"");
            assertThat(json).contains("\"value\":42");
        }

        @Test
        @DisplayName("성공: null 객체 직렬화 시 \"null\" 문자열을 반환한다")
        void serialize_nullObject_returnsNullString() {
            // when
            String result = DataSerializer.serialize(null);

            // then
            assertThat(result).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("deserialize(String, Class) - JSON 역직렬화")
    class DeserializeStringTest {

        @Test
        @DisplayName("성공: JSON 문자열을 객체로 역직렬화한다")
        void deserialize_validJson_returnsObject() {
            // given
            String json = "{\"name\":\"test\",\"value\":100}";

            // when
            TestDto result = DataSerializer.deserialize(json, TestDto.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
            assertThat(result.getValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("성공: 잘못된 JSON은 null을 반환한다 (예외 발생 없음)")
        void deserialize_invalidJson_returnsNull() {
            // given
            String invalidJson = "not a json";

            // when
            TestDto result = DataSerializer.deserialize(invalidJson, TestDto.class);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("성공: 알 수 없는 필드가 있어도 역직렬화에 성공한다")
        void deserialize_unknownFields_succeeds() {
            // given
            String json = "{\"name\":\"test\",\"value\":1,\"unknownField\":\"ignored\"}";

            // when
            TestDto result = DataSerializer.deserialize(json, TestDto.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("deserialize(Object, Class) - 객체 변환")
    class DeserializeObjectTest {

        @Test
        @DisplayName("성공: Map 객체를 DTO로 변환한다")
        void deserialize_mapToDto_succeeds() {
            // given
            java.util.Map<String, Object> map = java.util.Map.of("name", "converted", "value", 99);

            // when
            TestDto result = DataSerializer.deserialize(map, TestDto.class);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("converted");
            assertThat(result.getValue()).isEqualTo(99);
        }
    }

    // 테스트용 DTO
    static class TestDto {
        private String name;
        private int value;

        public TestDto() {}

        public TestDto(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public int getValue() { return value; }
    }
}
