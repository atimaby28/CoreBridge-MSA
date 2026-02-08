package halo.corebridge.jobpostinghot.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchStatusController {

    private final JobExplorer jobExplorer;

    /**
     * 배치 실행 이력 조회
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getJobHistory(
            @RequestParam(defaultValue = "10") int limit) {

        List<JobExecution> executions = jobExplorer.findJobInstancesByJobName("hotScoreJob", 0, limit)
                .stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(limit)
                .toList();

        List<Map<String, Object>> result = executions.stream()
                .map(exec -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("executionId", exec.getId());
                    map.put("status", exec.getStatus().toString());
                    map.put("startTime", exec.getStartTime());
                    map.put("endTime", exec.getEndTime());
                    map.put("trigger", exec.getJobParameters().getString("trigger"));

                    exec.getStepExecutions().forEach(step -> {
                        map.put("readCount", step.getReadCount());
                        map.put("writeCount", step.getWriteCount());
                        map.put("skipCount", step.getSkipCount());
                    });

                    if (exec.getEndTime() != null && exec.getStartTime() != null) {
                        long durationMs = java.time.Duration.between(
                                exec.getStartTime(), exec.getEndTime()).toMillis();
                        map.put("durationMs", durationMs);
                    }

                    return map;
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}
