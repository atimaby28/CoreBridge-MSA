package halo.corebridge.jobpostingread.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/circuit-breakers")
@RequiredArgsConstructor
public class CircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> result = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            Map<String, Object> status = new HashMap<>();
            status.put("state", cb.getState().name());
            status.put("failureRate", metrics.getFailureRate());
            status.put("slowCallRate", metrics.getSlowCallRate());
            status.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
            status.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            status.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            status.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
            result.put(cb.getName(), status);
        });

        return ResponseEntity.ok(result);
    }
}
