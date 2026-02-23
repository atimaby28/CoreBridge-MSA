package halo.corebridge.jobpostingcomment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostingcomment",
        "halo.corebridge.common"
})
@EnableJpaRepositories(basePackages = {
        "halo.corebridge.jobpostingcomment",
        "halo.corebridge.common.outboxmessagerelay"
})
@EntityScan(basePackages = {
        "halo.corebridge.jobpostingcomment",
        "halo.corebridge.common.outboxmessagerelay"
})
@EnableAsync
public class JobpostingCommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingCommentApplication.class, args);
    }
}
