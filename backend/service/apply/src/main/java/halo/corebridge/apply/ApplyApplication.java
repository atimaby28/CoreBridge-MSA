package halo.corebridge.apply;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.apply",
        "halo.corebridge.common"
})
@EnableJpaRepositories(basePackages = {
        "halo.corebridge.apply",
        "halo.corebridge.common.outboxmessagerelay"
})
@EntityScan(basePackages = {
        "halo.corebridge.apply",
        "halo.corebridge.common.outboxmessagerelay"
})
@EnableAsync
public class ApplyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApplyApplication.class, args);
    }
}
