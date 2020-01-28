package uk.gov.justice.hmpps.datacompliance.services.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.ImageUploadBatchRepository;
import uk.gov.justice.hmpps.datacompliance.services.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OffenderImageMigrationIntegrationTest extends IntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockBean
    private ImageRecognitionClient imageRecognitionClient;

    @Autowired
    private OffenderIterator iterator;

    @Autowired
    private OffenderImageUploaderFactory uploaderFactory;

    @Autowired
    private ImageUploadBatchRepository repository;

    @Autowired
    private TimeSource timeSource;

    private OffenderImageMigration migration;

    @BeforeEach
    void set() {
        migration = new OffenderImageMigration(iterator, uploaderFactory, repository, timeSource, "some cron");
    }

    @Test
    void runMigration() {

        when(imageRecognitionClient.uploadImageToCollection(any(), any(), anyLong()))
                .thenReturn(Optional.of("face1"))
                .thenReturn(Optional.of("face2"));

        oauthApiMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"123\",\"token_type\":\"bearer\",\"expires_in\":\"999999\"}")
                .setHeader("Content-Type", "application/json"));
        elite2ApiMock.setDispatcher(mockElite2ApiResponses());

        migration.run();

        verify(imageRecognitionClient).uploadImageToCollection(new byte[]{0x01}, new OffenderNumber("offender1"), 1L);
        verify(imageRecognitionClient).uploadImageToCollection(new byte[]{0x02}, new OffenderNumber("offender2"), 2L);
        verifyNoMoreInteractions(imageRecognitionClient);

        var persistedBatch = repository.findAll().iterator().next();
        assertThat(persistedBatch.getUploadCount()).isEqualTo(2);
    }

    private Dispatcher mockElite2ApiResponses() {

        return new Dispatcher() {

            @SneakyThrows
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {

                var path = requireNonNull(request.getPath());
                var offenderIdsMatch = compile("^/api/offenders/ids$").matcher(path);
                var imageMetaDataMatch = compile("^/api/images/offenders/.*([1-9])+$").matcher(path);
                var imageDataMatch = compile("^/api/images/([1-9]+)/data$").matcher(path);

                if (offenderIdsMatch.find()) {

                    return new MockResponse()
                            .setBody(OBJECT_MAPPER.writeValueAsString(List.of(
                                    new OffenderNumber("offender1"),
                                    new OffenderNumber("offender2"))))
                            .setHeader("Content-Type", "application/json")
                            .setHeader("Total-Records", "2");

                } else if (imageMetaDataMatch.find()) {

                    return new MockResponse()
                            .setBody(OBJECT_MAPPER.writeValueAsString(List.of(
                                    new OffenderImageMetadata(parseLong(imageMetaDataMatch.group(1)), "FACE"),
                                    new OffenderImageMetadata(123L, "OTHER"))))
                            .setHeader("Content-Type", "application/json");

                } else if (imageDataMatch.find()) {

                    return new MockResponse()
                            .setBody(new Buffer().write(new byte[]{(byte) parseInt(imageDataMatch.group(1)) }))
                            .setHeader("Content-Type", "image/jpeg");

                }

                return new MockResponse().setResponseCode(404);
            }
        };
    }
}