package halo.corebridge.common.snowflake;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Snowflake ID 생성기 설정
 */
@Configuration
public class SnowflakeConfig {

    @Value("${snowflake.node-id:#{null}}")
    private Long nodeId;

    @Bean
    public Snowflake snowflake() {
        if (nodeId != null) {
            return new Snowflake(nodeId);
        }
        return new Snowflake();
    }
}
