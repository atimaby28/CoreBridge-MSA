package halo.corebridge.jobpostinghot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostinghot",
        "halo.corebridge.common"
})
public class JobpostingHotApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingHotApplication.class, args);
    }
}
