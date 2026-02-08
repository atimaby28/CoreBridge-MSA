package halo.corebridge.jobpostinghot.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.Duration;

@Slf4j
public class HotScoreJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("========================================");
        log.info("[Batch] hotScoreJob STARTED - executionId={}", jobExecution.getId());
        log.info("========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();
        Duration duration = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
        );

        log.info("========================================");
        log.info("[Batch] hotScoreJob {} - executionId={}, duration={}ms",
                status, jobExecution.getId(), duration.toMillis());

        // Step 실행 결과 요약
        jobExecution.getStepExecutions().forEach(step -> {
            log.info("[Batch] Step '{}': read={}, written={}, filtered={}, skipped={}, status={}",
                    step.getStepName(),
                    step.getReadCount(),
                    step.getWriteCount(),
                    step.getFilterCount(),
                    step.getSkipCount(),
                    step.getStatus()
            );
        });

        if (status == BatchStatus.FAILED) {
            log.error("[Batch] hotScoreJob FAILED - check exceptions:");
            jobExecution.getAllFailureExceptions()
                    .forEach(ex -> log.error("[Batch] Failure: {}", ex.getMessage()));
        }

        log.info("========================================");
    }
}
