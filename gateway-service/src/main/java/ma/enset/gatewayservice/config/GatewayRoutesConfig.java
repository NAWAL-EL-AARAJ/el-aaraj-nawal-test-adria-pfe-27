package ma.enset.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("account-service", r -> r
                        .path("/api/accounts/**")
                        .uri("lb://ACCOUNT-SERVICE"))
                .route("transaction-service", r -> r
                        .path("/api/transactions/**")
                        .uri("lb://TRANSACTION-SERVICE"))
                .route("ai-agent-service", r -> r
                        .path("/api/chat/**")
                        .uri("lb://AI-AGENT-SERVICE"))
                .route("account-graphql", r -> r
                        .path("/graphql/accounts")
                        .filters(f -> f.setPath("/graphql"))
                        .uri("lb://ACCOUNT-SERVICE"))
                .route("transaction-graphql", r -> r
                        .path("/graphql/transactions")
                        .filters(f -> f.setPath("/graphql"))
                        .uri("lb://TRANSACTION-SERVICE"))
                .build();
    }

}
