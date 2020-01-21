package uk.gov.justice.hmpps.datacompliance.services.client.elite2api;

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
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

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

        var offenderNumbers = List.of(
                new OffenderNumber("offender1"),
                new OffenderNumber("offender2"));

        elite2ApiMock.enqueue(new MockResponse()
                .setBody(OBJECT_MAPPER.writeValueAsString(offenderNumbers))
                .setHeader("Content-Type", "application/json")
                .setHeader("Total-Records", "123"));

        var response = elite2ApiClient.getOffenderNumbers(0, 2);

        assertThat(response.getOffenderNumbers()).extracting(OffenderNumber::getOffenderNumber)
                .containsExactlyInAnyOrder("offender1", "offender2");
        assertThat(response.getTotalCount()).isEqualTo(123);

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
    void getOffenderNumbersThrowsIfResponseContainsNulls() {

        elite2ApiMock.enqueue(new MockResponse()
                .setBody("[{\"offenderNumber\":null}]")
                .setHeader("Content-Type", "application/json")
                .setHeader("Total-Records", "123"));

        assertThatThrownBy(() -> elite2ApiClient.getOffenderNumbers(0, 2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Null offender number");
    }
}