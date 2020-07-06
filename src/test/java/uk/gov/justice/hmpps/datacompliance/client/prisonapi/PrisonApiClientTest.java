package uk.gov.justice.hmpps.datacompliance.client.prisonapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.PendingDeletionsRequest;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PrisonApiClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 2, 1, 3, 4, 5, 123456789);
    private static final long PAGE_LIMIT = 2;

    private final MockWebServer prisonApiMock = new MockWebServer();
    private PrisonApiClient prisonApiClient;

    @BeforeEach
    void initialize() {
        prisonApiClient = new PrisonApiClient(WebClient.create(),
                DataComplianceProperties.builder()
                        .prisonApiBaseUrl(format("http://localhost:%s", prisonApiMock.getPort()))
                        .prisonApiOffenderIdsIterationThreads(1)
                        .prisonApiOffenderIdsLimit(PAGE_LIMIT)
                        .prisonApiOffenderIdsInitialOffset(0L)
                        .build());
    }

    @AfterEach
    void tearDown() throws Exception {
        prisonApiMock.shutdown();
    }

    @Test
    void getOffenderNumbers() throws Exception {

        var offenderNumbers = List.of(
                new OffenderNumber("A1234AA"),
                new OffenderNumber("B1234BB"));

        prisonApiMock.enqueue(new MockResponse()
                .setBody(OBJECT_MAPPER.writeValueAsString(offenderNumbers))
                .setHeader("Content-Type", "application/json")
                .setHeader("Total-Records", "123"));

        var response = prisonApiClient.getOffenderNumbers(0, PAGE_LIMIT);

        assertThat(response.getOffenderNumbers()).extracting(OffenderNumber::getOffenderNumber)
                .containsExactlyInAnyOrder("A1234AA", "B1234BB");
        assertThat(response.getTotalCount()).isEqualTo(123);

        RecordedRequest recordedRequest = prisonApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/offenders/ids");
        assertThat(recordedRequest.getHeader("Page-Offset")).isEqualTo("0");
        assertThat(recordedRequest.getHeader("Page-Limit")).isEqualTo("2");
    }

    @Test
    void getOffenderNumbersThrowsOnNonSuccessResponse() {

        prisonApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> prisonApiClient.getOffenderNumbers(0, PAGE_LIMIT))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void getOffenderNumbersThrowsIfResponseContainsNulls() {

        prisonApiMock.enqueue(new MockResponse()
                .setBody("[{\"offenderNumber\":null}]")
                .setHeader("Content-Type", "application/json")
                .setHeader("Total-Records", "123"));

        assertThatThrownBy(() -> prisonApiClient.getOffenderNumbers(0, PAGE_LIMIT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Null offender number");
    }

    @Test
    void getOffenderFaceImages() throws Exception {

        var offenderImages = List.of(new OffenderImageMetadata(123L, "FACE"), new OffenderImageMetadata(456L, "OTHER"));

        prisonApiMock.enqueue(new MockResponse()
                .setBody(OBJECT_MAPPER.writeValueAsString(offenderImages))
                .setHeader("Content-Type", "application/json"));

        var result = prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber("A1234AA"));

        assertThat(result).containsOnly(new OffenderImageMetadata(123L, "FACE"));

        RecordedRequest recordedRequest = prisonApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/images/offenders/A1234AA");
    }

    @Test
    void getOffenderFaceThrowsOnNonSuccessResponse() {

        prisonApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber("A1234AA")))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void getImageData() throws Exception {

        final var data = new byte[]{0x12};

        prisonApiMock.enqueue(new MockResponse()
                .setBody(new Buffer().write(data))
                .setHeader("Content-Type", "image/jpeg"));

        var result = prisonApiClient.getImageData(123L);

        assertThat(result).contains(data);

        RecordedRequest recordedRequest = prisonApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/images/123/data");
    }

    @Test
    void getImageDataHandlesMissingImageData() {

        prisonApiMock.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Not Found\"}")
                .setResponseCode(404));

        assertThat(prisonApiClient.getImageData(123L)).isEmpty();
    }

    @Test
    void getImageDataThrowsOnNonSuccessResponse() {

        prisonApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> prisonApiClient.getImageData(123L))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void requestPendingDeletions() throws Exception {

        prisonApiMock.enqueue(new MockResponse().setResponseCode(202));

        prisonApiClient.requestPendingDeletions(PendingDeletionsRequest.builder()
                .dueForDeletionWindowStart(TIMESTAMP)
                .dueForDeletionWindowEnd(TIMESTAMP.plusSeconds(1))
                .batchId(123L)
                .limit(1)
                .build());

        RecordedRequest recordedRequest = prisonApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/data-compliance/offenders/pending-deletions");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{" +
                "\"dueForDeletionWindowStart\":\"2020-02-01T03:04:05.123456\"," +
                "\"dueForDeletionWindowEnd\":\"2020-02-01T03:04:06.123456\"," +
                "\"batchId\":123," +
                "\"limit\":1" +
                "}");
    }

    @Test
    void requestPendingDeletionsThrowsOnNonSuccessResponse() {

        prisonApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> prisonApiClient.requestPendingDeletions(PendingDeletionsRequest.builder().build()))
                .isInstanceOf(WebClientResponseException.class);
    }
}
