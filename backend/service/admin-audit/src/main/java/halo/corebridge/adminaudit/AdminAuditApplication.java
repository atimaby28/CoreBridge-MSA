package halo.corebridge.adminaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "halo.corebridge.adminaudit",
        "halo.corebridge.common"
})
public class AdminAuditApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminAuditApplication.class, args);
    }
}
