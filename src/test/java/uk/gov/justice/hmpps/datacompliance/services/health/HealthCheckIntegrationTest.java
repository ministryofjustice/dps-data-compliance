package uk.gov.justice.hmpps.datacompliance.services.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static java.util.Objects.requireNonNull;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration
public class HealthCheckIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthPageReportsOk() {
        var response = restTemplate.getForEntity("/health", String.class);
        assertThatJson(requireNonNull(response.getBody())).node("status").isEqualTo("UP");
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    void healthPingPageIsAvailable() {
        var response = restTemplate.getForEntity("/health/ping", String.class);
        assertThatJson(requireNonNull(response.getBody())).node("status").isEqualTo("UP");
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }
}
