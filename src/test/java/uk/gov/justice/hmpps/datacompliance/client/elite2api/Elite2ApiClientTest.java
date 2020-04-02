package uk.gov.justice.hmpps.datacompliance.client.elite2api;

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
import uk.gov.justice.hmpps.datacompliance.client.elite2api.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class Elite2ApiClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2020, 2, 1, 3, 4, 5, 123456789);
    private static final long PAGE_LIMIT = 2;

    private final MockWebServer elite2ApiMock = new MockWebServer();
    private Elite2ApiClient elite2ApiClient;

    @BeforeEach
    void initialize() {
        elite2ApiClient = new Elite2ApiClient(WebClient.create(),
                DataComplianceProperties.builder()
                        .elite2ApiBaseUrl(format("http://localhost:%s", elite2ApiMock.getPort()))
                        .elite2ApiOffenderIdsIterationThreads(1)
                        .elite2ApiOffenderIdsLimit(PAGE_LIMIT)
                        .elite2ApiOffenderIdsInitialOffset(0L)
                        .build());
    }

    @AfterEach
    void tearDown() throws Exception {
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

        var response = elite2ApiClient.getOffenderNumbers(0, PAGE_LIMIT);

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

        assertThatThrownBy(() -> elite2ApiClient.getOffenderNumbers(0, PAGE_LIMIT))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void getOffenderNumbersThrowsIfResponseContainsNulls() {

        elite2ApiMock.enqueue(new MockResponse()
                .setBody("[{\"offenderNumber\":null}]")
                .setHeader("Content-Type", "application/json")
                .setHeader("Total-Records", "123"));

        assertThatThrownBy(() -> elite2ApiClient.getOffenderNumbers(0, PAGE_LIMIT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Null offender number");
    }

    @Test
    void getOffenderFaceImages() throws Exception {

        var offenderImages = List.of(new OffenderImageMetadata(123L, "FACE"), new OffenderImageMetadata(456L, "OTHER"));

        elite2ApiMock.enqueue(new MockResponse()
                .setBody(OBJECT_MAPPER.writeValueAsString(offenderImages))
                .setHeader("Content-Type", "application/json"));

        var result = elite2ApiClient.getOffenderFaceImagesFor(new OffenderNumber("offender1"));

        assertThat(result).containsOnly(new OffenderImageMetadata(123L, "FACE"));

        RecordedRequest recordedRequest = elite2ApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/images/offenders/offender1");
    }

    @Test
    void getOffenderFaceThrowsOnNonSuccessResponse() {

        elite2ApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> elite2ApiClient.getOffenderFaceImagesFor(new OffenderNumber("offender1")))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void getImageData() throws Exception {

        final var data = new byte[]{0x12};

        elite2ApiMock.enqueue(new MockResponse()
                .setBody(new Buffer().write(data))
                .setHeader("Content-Type", "image/jpeg"));

        var result = elite2ApiClient.getImageData(123L);

        assertThat(result).contains(data);

        RecordedRequest recordedRequest = elite2ApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/images/123/data");
    }

    @Test
    void getImageDataHandlesMissingImageData() {

        elite2ApiMock.enqueue(new MockResponse()
                .setResponseCode(404));

        assertThat(elite2ApiClient.getImageData(123L)).isEmpty();
    }

    @Test
    void getImageDataThrowsOnNonSuccessResponse() {

        elite2ApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> elite2ApiClient.getImageData(123L))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void requestPendingDeletions() throws Exception {

        elite2ApiMock.enqueue(new MockResponse().setResponseCode(202));

        elite2ApiClient.requestPendingDeletions(TIMESTAMP, TIMESTAMP.plusSeconds(1), "request1");

        RecordedRequest recordedRequest = elite2ApiMock.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/data-compliance/offenders/pending-deletions");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{" +
                "\"dueForDeletionWindowStart\":\"2020-02-01T03:04:05.123456\"," +
                "\"dueForDeletionWindowEnd\":\"2020-02-01T03:04:06.123456\"," +
                "\"requestId\":\"request1" +
                "\"}");
    }

    @Test
    void requestPendingDeletionsThrowsOnNonSuccessResponse() {

        elite2ApiMock.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> elite2ApiClient.requestPendingDeletions(TIMESTAMP, TIMESTAMP.plusSeconds(1), "request1"))
                .isInstanceOf(WebClientResponseException.class);
    }
}