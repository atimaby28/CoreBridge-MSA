package halo.corebridge.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.notification",
        "halo.corebridge.common"
})
@EnableJpaRepositories(basePackages = {
        "halo.corebridge.notification",
        "halo.corebridge.common.event.idempotency"
})
@EntityScan(basePackages = {
        "halo.corebridge.notification",
        "halo.corebridge.common.event.idempotency"
})
@EnableJpaAuditing
@EnableScheduling
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
