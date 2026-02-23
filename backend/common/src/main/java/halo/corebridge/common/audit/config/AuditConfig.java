package halo.corebridge.common.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * 감사 로그 관련 설정
 */
@Configuration
@EnableAsync
public class AuditConfig {

    /**
     * 감사 로그 전송용 비동기 실행자
     * 메인 스레드와 분리하여 성능 영향 최소화
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 큐가 가득 찬 경우 로그만 남기고 무시
            // 감사 로그 실패가 비즈니스 로직에 영향을 주면 안 됨
        });
        executor.initialize();
        return executor;
    }

    /**
     * admin-audit 서비스 호출용 RestTemplate
     */
    @Bean(name = "auditRestTemplate")
    public RestTemplate auditRestTemplate() {
        return new RestTemplate();
    }
}
