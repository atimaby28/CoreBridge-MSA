package halo.corebridge.jobpostinghot.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotJobpostingScheduler {

    private final JobLauncher jobLauncher;
    private final Job hotScoreJob;

    /**
     * 서비스 시작 시 인기 공고 갱신
     */
    @PostConstruct
    public void initOnStartup() {
        log.info("=== Starting initial hot jobposting batch on startup ===");
        runBatchJob("startup");
    }

    /**
     * 매시간 인기 공고 갱신
     */
    @Scheduled(cron = "0 0 * * * *")
    public void updateHotJobpostingsHourly() {
        log.info("=== Starting hourly hot jobposting batch ===");
        runBatchJob("hourly");
    }

    /**
     * 매일 자정 인기 공고 갱신
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateHotJobpostingsDaily() {
        log.info("=== Starting daily hot jobposting batch ===");
        runBatchJob("daily");
    }

    private void runBatchJob(String trigger) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("date", LocalDate.now().toString())
                    .addString("trigger", trigger)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(hotScoreJob, params);
        } catch (Exception e) {
            log.error("=== Batch job failed (trigger={}): {} ===", trigger, e.getMessage());
        }
    }
}
