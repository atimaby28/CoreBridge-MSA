package halo.corebridge.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Value("${spring.application.name:corebridge-service}")
    private String applicationName;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoreBridge - " + applicationName)
                        .description("CoreBridge MSA - " + applicationName + " API 명세")
                        .version("1.0.0"));
    }
}
