package uk.gov.justice.hmpps.datacompliance.services.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;

import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class Elite2ApiClientTest {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static MockWebServer elite2ApiMock;
    private Elite2ApiClient elite2ApiClient;

    @BeforeAll
    static void setUp() throws Exception {
        elite2ApiMock = new MockWebServer();
        elite2ApiMock.start();
    }

    @BeforeEach
    void initialize() {
        elite2ApiClient = new Elite2ApiClient(WebClient.create(),
                new DataComplianceProperties(format("http://localhost:%s", elite2ApiMock.getPort())));
    }

    @AfterAll
    static void tearDown() throws Exception {
        elite2ApiMock.shutdown();
    }

    @Test
    void getOffenderNumbers() throws Exception {

        List<Elite2ApiClient.OffenderNumber> offenderNumbers = List.of(
                new Elite2ApiClient.OffenderNumber("offender1"),
                new Elite2ApiClient.OffenderNumber("offender2"));

        elite2ApiMock.enqueue(new MockResponse()
                .setBody(OBJECT_MAPPER.writeValueAsString(offenderNumbers))
                .setHeader("Content-Type", "application/json"));

        assertThat(elite2ApiClient.getOffenderNumbers(0, 2))
                .containsExactlyInAnyOrder("offender1", "offender2");

        RecordedRequest recordedRequest = elite2ApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/offenders/ids");
        assertThat(recordedRequest.getHeader("Page-Offset")).isEqualTo("0");
        assertThat(recordedRequest.getHeader("Page-Limit")).isEqualTo("2");
    }

    @Test
    void getOffenderNumbersThrowsOnNonSuccessResponse() {

        elite2ApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> elite2ApiClient.getOffenderNumbers(0, 2))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void getOffenderNumbersThrowsIfResponseContainsNulls() throws Exception {

        elite2ApiMock.enqueue(new MockResponse()
                .setBody(OBJECT_MAPPER.writeValueAsString(List.of(new Elite2ApiClient.OffenderNumber(null))))
                .setHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> elite2ApiClient.getOffenderNumbers(0, 2))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Response contained null offender numbers");
    }
}