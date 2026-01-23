package halo.corebridge.jobpostingcomment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.jobpostingcomment",
        "halo.corebridge.common"
})
@EnableAsync
public class JobpostingCommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobpostingCommentApplication.class, args);
    }
}
