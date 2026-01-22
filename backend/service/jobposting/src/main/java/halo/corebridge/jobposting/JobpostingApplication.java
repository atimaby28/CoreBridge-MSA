package halo.corebridge.jobposting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobposting",
        "halo.corebridge.common"
})
@EnableAsync
public class JobpostingApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingApplication.class, args);
    }
}
