package ma.enset.transactionservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Expose l'état des circuit breakers dans /actuator/health (composant "circuitBreakers").
 * L'indicateur fourni par resilience4j-spring-boot3 ne s'enregistre pas avec Spring Boot 4.
 * Un circuit ouvert n'est pas une panne de transaction-service : il est signalé sans passer le service à DOWN.
 */
@Component("circuitBreakers")
@RequiredArgsConstructor
public class CircuitBreakersHealthIndicator extends AbstractHealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean anyOpen = false;
        for (var circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            var metrics = circuitBreaker.getMetrics();
            var state = circuitBreaker.getState();
            anyOpen |= state == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
            builder.withDetail(circuitBreaker.getName(), java.util.Map.of(
                    "state", state.name(),
                    "failureRate", metrics.getFailureRate() + "%",
                    "bufferedCalls", metrics.getNumberOfBufferedCalls(),
                    "failedCalls", metrics.getNumberOfFailedCalls(),
                    "notPermittedCalls", metrics.getNumberOfNotPermittedCalls()));
        }
        builder.status(anyOpen ? new Status("CIRCUIT_OPEN") : Status.UP);
    }
}
