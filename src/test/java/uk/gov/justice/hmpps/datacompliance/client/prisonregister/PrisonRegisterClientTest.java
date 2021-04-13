package uk.gov.justice.hmpps.datacompliance.client.prisonregister;

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

import java.time.Duration;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PrisonRegisterClientTest {

    private final MockWebServer mockPrisonRegisterWebServer = new MockWebServer();
    private PrisonRegisterClient prisonRegisterClient;


    @BeforeEach
    public void setup() {
        prisonRegisterClient = new PrisonRegisterClient(WebClient.create(),
            DataComplianceProperties.builder()
                .prisonRegisterBaseUrl(format("http://localhost:%s", mockPrisonRegisterWebServer.getPort()))
                .prisonRegisterTimeout(Duration.ofSeconds(5))
                .build());
    }

    @AfterEach
    void tearDown() throws Exception {
        mockPrisonRegisterWebServer.shutdown();
    }


    @Test
    void retrieveOmuContactEmail() {

        mockPrisonRegisterWebServer.enqueue(new MockResponse()
            .setBody("someEmail@OMU.co.uk")
            .setResponseCode(HttpStatus.OK.value()));

        assertThat(prisonRegisterClient.retrieveOmuContactEmail("MDI")).get().isEqualTo("someEmail@OMU.co.uk");
    }

    @Test
    void retrieveOmuContactEmailEmptyWhenNotFound() {

        mockPrisonRegisterWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value()));

        assertThat(prisonRegisterClient.retrieveOmuContactEmail("MDI")).isEmpty();
    }


    @Test
    void retrieveOmuContactEmailThrowsOnUnexpectedStatus() {

        mockPrisonRegisterWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.PAYMENT_REQUIRED.value()));

        assertThatThrownBy(() -> prisonRegisterClient.retrieveOmuContactEmail("MDI"))
            .isInstanceOf(WebClientResponseException.class)
            .hasMessageContaining("Payment Required");
    }

}