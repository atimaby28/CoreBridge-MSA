package halo.corebridge.jobpostingread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostingread",
        "halo.corebridge.common"
})
@EnableAsync
public class JobpostingReadApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingReadApplication.class, args);
    }
}
