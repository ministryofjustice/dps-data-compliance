package uk.gov.justice.hmpps.datacompliance.client.pathfinder;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.time.Duration;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PathfinderApiClientTest {

    private final MockWebServer pathfinderApiMock = new MockWebServer();
    private PathfinderApiClient pathfinderApiClient;


    @BeforeEach
    void initialize() {
        pathfinderApiClient = new PathfinderApiClient(WebClient.create(),
                DataComplianceProperties.builder()
                        .pathfinderApiBaseUrl(format("http://localhost:%s", pathfinderApiMock.getPort()))
                        .pathfinderApiTimeout(Duration.ofSeconds(5))
                        .build());
    }

    @AfterEach
    void tearDown() throws Exception {
        pathfinderApiMock.shutdown();
    }

    @Test
    void isReferredToPathfinderTrueWhenSuccessResponse() {

        pathfinderApiMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value()));

        assertThat(pathfinderApiClient.isReferredToPathfinder(new OffenderNumber("A1234AA")))
                .isTrue();
    }

    @Test
    void isReferredToPathfinderFalseWhenNotFound() {

        pathfinderApiMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        assertThat(pathfinderApiClient.isReferredToPathfinder(new OffenderNumber("A1234AA")))
                .isFalse();
    }

    @Test
    void isReferredToPathfinderThrowsOnUnexpectedStatus() {

        pathfinderApiMock.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.I_AM_A_TEAPOT.value()));

        assertThatThrownBy(() -> pathfinderApiClient.isReferredToPathfinder(new OffenderNumber("A1234AA")))
                .isInstanceOf(WebClientResponseException.class)
                .hasMessageContaining("I'm a teapot");
    }
}
