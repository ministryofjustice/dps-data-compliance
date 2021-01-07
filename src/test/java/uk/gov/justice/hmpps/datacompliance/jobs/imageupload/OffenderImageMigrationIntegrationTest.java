package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffendersWithImagesResponse;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageUploadBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.FACE_NOT_FOUND;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.error;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.success;

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
        migration = new OffenderImageMigration(iterator, uploaderFactory, repository, timeSource);
    }

    @Test
    void runMigration() {

        when(imageRecognitionClient.uploadImageToCollection(any()))
                .thenReturn(success(new FaceId("face1")))
                .thenReturn(success(new FaceId("face2")))
                .thenReturn(error(FACE_NOT_FOUND));

        hmppsAuthMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"123\",\"token_type\":\"bearer\",\"expires_in\":\"999999\"}")
                .setHeader("Content-Type", "application/json"));
        prisonApiMock.setDispatcher(mockPrisonApiResponses());

        migration.run();

        verify(imageRecognitionClient).uploadImageToCollection(offenderImage(offenderNo(1), 1L, new byte[]{0x01}));
        verify(imageRecognitionClient).uploadImageToCollection(offenderImage(offenderNo(2), 2L, new byte[]{0x02}));
        verify(imageRecognitionClient).uploadImageToCollection(offenderImage(offenderNo(3), 3L, new byte[]{0x03}));
        verifyNoMoreInteractions(imageRecognitionClient);

        var persistedBatch = repository.findAll().iterator().next();
        assertThat(persistedBatch.getUploadCount()).isEqualTo(2);
    }

    @Test
    @Sql("image_upload_batch.sql")
    void runMigrationAsAnUpdate() {

        when(imageRecognitionClient.uploadImageToCollection(any()))
                .thenReturn(success(new FaceId("face1")))
                .thenReturn(success(new FaceId("face2")))
                .thenReturn(error(FACE_NOT_FOUND));

        hmppsAuthMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"123\",\"token_type\":\"bearer\",\"expires_in\":\"999999\"}")
                .setHeader("Content-Type", "application/json"));
        prisonApiMock.setDispatcher(mockPrisonApiResponses());

        migration.run();

        verify(imageRecognitionClient).uploadImageToCollection(offenderImage(offenderNo(4), 4L, new byte[]{0x04}));
        verify(imageRecognitionClient).uploadImageToCollection(offenderImage(offenderNo(5), 5L, new byte[]{0x05}));
        verify(imageRecognitionClient).uploadImageToCollection(offenderImage(offenderNo(6), 6L, new byte[]{0x06}));
        verifyNoMoreInteractions(imageRecognitionClient);

        var persistedBatch = repository.findFirstByBatchIdNotOrderByUploadStartDateTimeDesc(1).get();
        assertThat(persistedBatch.getUploadCount()).isEqualTo(2);
    }

    private Dispatcher mockPrisonApiResponses() {

        return new Dispatcher() {

            @SneakyThrows
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {

                var path = requireNonNull(request.getPath());
                var offenderIdsMatch = compile("^/api/offenders/ids$").matcher(path);
                var offendersWithImagesMatch = compile("^/api/data-compliance/offenders-with-images.*$").matcher(path);
                var imageMetaDataMatch = compile("^/api/images/offenders/A([0-9]){4}AA$").matcher(path);
                var imageDataMatch = compile("^/api/images/([1-9]+)/data$").matcher(path);

                if (offenderIdsMatch.find()) {

                    return new MockResponse()
                            .setBody(OBJECT_MAPPER.writeValueAsString(List.of(
                                    offenderNo(1),
                                    offenderNo(2),
                                    offenderNo(3))))
                            .setHeader("Content-Type", "application/json")
                            .setHeader("Total-Records", "3");

                } else if (offendersWithImagesMatch.find()) {

                    return new MockResponse()
                            .setBody(OBJECT_MAPPER.writeValueAsString(OffendersWithImagesResponse.builder()
                                            .offenderNumber(offenderNo(4))
                                            .offenderNumber(offenderNo(5))
                                            .offenderNumber(offenderNo(6))
                                            .totalElements(3L)
                                            .build()))
                            .setHeader("Content-Type", "application/json");

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

    private OffenderNumber offenderNo(final int index) {
        return new OffenderNumber(format("A%04dAA", index));
    }

    private OffenderImage offenderImage(final OffenderNumber offenderNumber, final long imageId, final byte[] data) {
        return OffenderImage.builder()
                .offenderNumber(offenderNumber)
                .imageId(imageId)
                .imageData(data)
                .build();
    }
}
