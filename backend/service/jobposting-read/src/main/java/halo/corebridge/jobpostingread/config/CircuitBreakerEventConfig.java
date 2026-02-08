package halo.corebridge.jobpostingread.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CircuitBreakerEventConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerListener);

        // 동적으로 생성되는 Circuit Breaker도 감지
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> registerListener(event.getAddedEntry()));
    }

    private void registerListener(CircuitBreaker circuitBreaker) {
        String name = circuitBreaker.getName();

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> logStateTransition(name, event))
                .onError(event -> log.debug(
                        "[CircuitBreaker] {} ERROR - duration={}ms, error={}",
                        name, event.getElapsedDuration().toMillis(), event.getThrowable().getMessage()))
                .onSuccess(event -> log.debug(
                        "[CircuitBreaker] {} SUCCESS - duration={}ms",
                        name, event.getElapsedDuration().toMillis()))
                .onCallNotPermitted(event -> log.warn(
                        "[CircuitBreaker] {} CALL_NOT_PERMITTED - circuit is OPEN, request rejected",
                        name));
    }

    private void logStateTransition(String name, CircuitBreakerOnStateTransitionEvent event) {
        log.warn("========================================");
        log.warn("[CircuitBreaker] {} STATE TRANSITION: {} -> {}",
                name, event.getStateTransition().getFromState(), event.getStateTransition().getToState());
        log.warn("========================================");
    }
}
