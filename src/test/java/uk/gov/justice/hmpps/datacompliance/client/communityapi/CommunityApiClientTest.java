package uk.gov.justice.hmpps.datacompliance.client.communityapi;

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
class CommunityApiClientTest {

    private final MockWebServer mockCommunityApiWebServer = new MockWebServer();
    private CommunityApiClient communityApiClient;


    @BeforeEach
    public void setup() {
        communityApiClient = new CommunityApiClient(WebClient.create(),
            DataComplianceProperties.builder()
                .communityApiBaseUrl(format("http://localhost:%s", mockCommunityApiWebServer.getPort()))
                .communityApiTimeout(Duration.ofSeconds(5))
                .build());
    }

    @AfterEach
    void tearDown() throws Exception {
        mockCommunityApiWebServer.shutdown();
    }


    @Test
    void isReferredForMappaTrueWhenSuccessResponse() {

        mockCommunityApiWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.OK.value()));

        assertThat(communityApiClient.isReferredForMappa(new OffenderNumber("A1234AA")))
            .isTrue();
    }

    @Test
    void isReferredForMappaFalseWhenNotFound() {

        mockCommunityApiWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value()));

        assertThat(communityApiClient.isReferredForMappa(new OffenderNumber("A1234AA")))
            .isFalse();
    }

    @Test
    void isReferredForMappaThrowsOnUnexpectedStatus() {

        mockCommunityApiWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.PAYMENT_REQUIRED.value()));

        assertThatThrownBy(() -> communityApiClient.isReferredForMappa(new OffenderNumber("A1234AA")))
            .isInstanceOf(WebClientResponseException.class)
            .hasMessageContaining("Payment Required");
    }
}