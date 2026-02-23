package halo.corebridge.jobpostingview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostingview",
        "halo.corebridge.common"
})
@EnableJpaRepositories(basePackages = {
        "halo.corebridge.jobpostingview",
        "halo.corebridge.common.outboxmessagerelay"
})
@EntityScan(basePackages = {
        "halo.corebridge.jobpostingview",
        "halo.corebridge.common.outboxmessagerelay"
})
@EnableAsync
public class JobpostingViewApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingViewApplication.class, args);
    }
}
