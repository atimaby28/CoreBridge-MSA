package halo.corebridge.jobpostingread.controller;

import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import halo.corebridge.jobpostingread.config.SecurityConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CircuitBreakerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, GatewayAuthenticationFilter.class, AuditLoggingFilter.class}
        )
)
@AutoConfigureDataJpa
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.autoconfigure.exclude=")
@DisplayName("CircuitBreakerController 테스트")
class CircuitBreakerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("성공: Circuit Breaker 상태를 조회한다")
    void getStatus_success() throws Exception {
        // given
        CircuitBreaker cb = mock(CircuitBreaker.class);
        CircuitBreaker.Metrics metrics = mock(CircuitBreaker.Metrics.class);

        given(cb.getName()).willReturn("jobpostingService");
        given(cb.getState()).willReturn(CircuitBreaker.State.CLOSED);
        given(cb.getMetrics()).willReturn(metrics);
        given(metrics.getFailureRate()).willReturn(0.0f);
        given(metrics.getSlowCallRate()).willReturn(0.0f);
        given(metrics.getNumberOfSuccessfulCalls()).willReturn(10);
        given(metrics.getNumberOfFailedCalls()).willReturn(0);
        given(metrics.getNumberOfNotPermittedCalls()).willReturn(0L);
        given(metrics.getNumberOfBufferedCalls()).willReturn(10);

        doReturn(Set.of(cb)).when(circuitBreakerRegistry).getAllCircuitBreakers();

        // when & then
        mockMvc.perform(get("/api/v1/circuit-breakers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobpostingService.state").value("CLOSED"))
                .andExpect(jsonPath("$.jobpostingService.numberOfSuccessfulCalls").value(10))
                .andExpect(jsonPath("$.jobpostingService.numberOfFailedCalls").value(0));
    }

    @Test
    @DisplayName("성공: Circuit Breaker가 없으면 빈 맵을 반환한다")
    void getStatus_empty() throws Exception {
        // given
        doReturn(Set.of()).when(circuitBreakerRegistry).getAllCircuitBreakers();

        // when & then
        mockMvc.perform(get("/api/v1/circuit-breakers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }
}
