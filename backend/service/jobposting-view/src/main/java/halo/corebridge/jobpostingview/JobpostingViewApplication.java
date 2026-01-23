package halo.corebridge.jobpostingview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostingview",
        "halo.corebridge.common"
})
@EnableAsync
public class JobpostingViewApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingViewApplication.class, args);
    }
}
