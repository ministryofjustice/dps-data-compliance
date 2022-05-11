package uk.gov.justice.hmpps.datacompliance.services.health;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PROTECTED)
abstract class HealthCheck implements HealthIndicator {

    private final WebClient client;

    @Override
    public Health health() {
        try {

            ResponseEntity<String> response = client.get()
                .uri("/health/ping")
                .retrieve()
                .toEntity(String.class)
                .block();

            return Health.up()
                .withDetail("HttpStatus", requireNonNull(response).getStatusCodeValue())
                .build();

        } catch (final Exception ex) {
            return Health.down(ex).build();
        }
    }

    @Component
    static class HmppsAuthHealth extends HealthCheck {
        protected HmppsAuthHealth(@Qualifier("hmppsAuthHealthWebClient") final WebClient client) {
            super(client);
        }
    }

    @Component
    static class PrisonApiHealth extends HealthCheck {
        protected PrisonApiHealth(@Qualifier("prisonApiHealthWebClient") final WebClient client) {
            super(client);
        }
    }

    @Component
    static class PathfinderApiHealth extends HealthCheck {
        protected PathfinderApiHealth(@Qualifier("pathfinderApiHealthWebClient") final WebClient client) {
            super(client);
        }
    }

    @Component
    static class CommunityApiHealth extends HealthCheck {
        protected CommunityApiHealth(@Qualifier("communityApiHealthWebClient") final WebClient client) {
            super(client);
        }
    }

    @Component
    static class PrisonRegisterHealth extends HealthCheck {
        protected PrisonRegisterHealth(@Qualifier("prisonRegisterHealthWebClient") final WebClient client) {
            super(client);
        }
    }
}
