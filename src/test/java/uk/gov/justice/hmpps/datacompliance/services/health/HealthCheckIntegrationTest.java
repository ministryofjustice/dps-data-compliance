package uk.gov.justice.hmpps.datacompliance.services.health;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;

import static org.hamcrest.core.StringContains.containsString;

public class HealthCheckIntegrationTest extends IntegrationTest {

    @Test
    void healthPageReportsOk() {

        mockExternalServiceResponseCode(200);

        webTestClient.get().uri("/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components['healthCheck.OAuthApiHealth'].status").isEqualTo("UP")
                .jsonPath("$.components['healthCheck.Elite2ApiHealth'].status").isEqualTo("UP")
                .jsonPath("$.components['healthCheck.PathfinderApiHealth'].status").isEqualTo("UP");
    }

    @Test
    void healthPingPageIsAvailable() {

        webTestClient.get().uri("/health/ping")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void healthPageReportsDown() {

        mockExternalServiceResponseCode(404);

        webTestClient.get().uri("/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo("DOWN")
                .jsonPath("$.components['healthCheck.OAuthApiHealth'].status").isEqualTo("DOWN")
                .jsonPath("$.components['healthCheck.OAuthApiHealth'].details.error").value(containsString("404 Not Found"))
                .jsonPath("$.components['healthCheck.Elite2ApiHealth'].status").isEqualTo("DOWN")
                .jsonPath("$.components['healthCheck.Elite2ApiHealth'].details.error").value(containsString("404 Not Found"))
                .jsonPath("$.components['healthCheck.PathfinderApiHealth'].status").isEqualTo("DOWN")
                .jsonPath("$.components['healthCheck.PathfinderApiHealth'].details.error").value(containsString("404 Not Found"));
    }

}
