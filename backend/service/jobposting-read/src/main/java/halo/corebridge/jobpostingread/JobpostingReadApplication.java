package halo.corebridge.jobpostingread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostingread",
        "halo.corebridge.common"
})
@EnableJpaRepositories(basePackages = {
        "halo.corebridge.jobpostingread",
        "halo.corebridge.common.event.idempotency"
})
@EntityScan(basePackages = {
        "halo.corebridge.jobpostingread",
        "halo.corebridge.common.event.idempotency"
})
@EnableAsync
public class JobpostingReadApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingReadApplication.class, args);
    }
}
