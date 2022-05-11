package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDatabaseDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

@ExtendWith(MockitoExtension.class)
class FalsePositiveCheckServiceTest {

    private static final String REFERENCE_OFFENDER_NO = "A1234AA";
    private static final String DUPLICATE_OFFENDER_NO = "B1234BB";
    private static final long REFERENCE_OFFENDER_IMAGE_ID_1 = 11;
    private static final long REFERENCE_OFFENDER_IMAGE_ID_2 = 12;
    private static final long DUPLICATE_OFFENDER_IMAGE_ID_1 = 21;
    private static final long DUPLICATE_OFFENDER_IMAGE_ID_2 = 22;
    private static final double IMAGE_SIMILARITY_THRESHOLD = 90.0;

    @Mock
    private PrisonApiClient prisonApiClient;

    @Mock
    private ImageDuplicationDetectionService imageDuplicationDetectionService;

    private FalsePositiveCheckService service;

    @BeforeEach
    void setUp() {
        service = new FalsePositiveCheckService(
            prisonApiClient,
            imageDuplicationDetectionService,
            DataComplianceProperties.builder()
                .falsePositiveDuplicateCheckEnabled(true)
                .falsePositiveDuplicateRequiredImageCount(2)
                .falsePositiveDuplicateImageSimilarityThreshold(IMAGE_SIMILARITY_THRESHOLD)
                .build());
    }

    @Test
    void falsePositive() {

        mockImagesFor(REFERENCE_OFFENDER_NO, REFERENCE_OFFENDER_IMAGE_ID_1, REFERENCE_OFFENDER_IMAGE_ID_2);
        mockImagesFor(DUPLICATE_OFFENDER_NO, DUPLICATE_OFFENDER_IMAGE_ID_1, DUPLICATE_OFFENDER_IMAGE_ID_2);

        when(imageDuplicationDetectionService.getSimilarity(any(), any()))
            .thenReturn(Optional.of(IMAGE_SIMILARITY_THRESHOLD - 1));

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isTrue();
    }

    @Test
    void notFalsePositiveIfImagesCannotBeCompared() {

        mockImagesFor(REFERENCE_OFFENDER_NO, REFERENCE_OFFENDER_IMAGE_ID_1, REFERENCE_OFFENDER_IMAGE_ID_2);
        mockImagesFor(DUPLICATE_OFFENDER_NO, DUPLICATE_OFFENDER_IMAGE_ID_1, DUPLICATE_OFFENDER_IMAGE_ID_2);

        when(imageDuplicationDetectionService.getSimilarity(any(), any())).thenReturn(Optional.empty());

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfSimilarityEqualsThreshold() {

        mockImagesFor(REFERENCE_OFFENDER_NO, REFERENCE_OFFENDER_IMAGE_ID_1, REFERENCE_OFFENDER_IMAGE_ID_2);
        mockImagesFor(DUPLICATE_OFFENDER_NO, DUPLICATE_OFFENDER_IMAGE_ID_1, DUPLICATE_OFFENDER_IMAGE_ID_2);

        when(imageDuplicationDetectionService.getSimilarity(any(), any()))
            .thenReturn(Optional.of(IMAGE_SIMILARITY_THRESHOLD));

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfSimilarityGreaterThanThreshold() {

        mockImagesFor(REFERENCE_OFFENDER_NO, REFERENCE_OFFENDER_IMAGE_ID_1, REFERENCE_OFFENDER_IMAGE_ID_2);
        mockImagesFor(DUPLICATE_OFFENDER_NO, DUPLICATE_OFFENDER_IMAGE_ID_1, DUPLICATE_OFFENDER_IMAGE_ID_2);

        when(imageDuplicationDetectionService.getSimilarity(any(), any()))
            .thenReturn(Optional.of(IMAGE_SIMILARITY_THRESHOLD + 1));

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfNoImagesForReferenceOffender() {

        when(prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber(REFERENCE_OFFENDER_NO))).thenReturn(emptyList());
        when(prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber(DUPLICATE_OFFENDER_NO)))
            .thenReturn(List.of(new OffenderImageMetadata(DUPLICATE_OFFENDER_IMAGE_ID_1, "FACE")));

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfNoImagesForDuplicateOffender() {

        when(prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber(REFERENCE_OFFENDER_NO)))
            .thenReturn(List.of(new OffenderImageMetadata(REFERENCE_OFFENDER_IMAGE_ID_1, "FACE")));
        when(prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber(DUPLICATE_OFFENDER_NO))).thenReturn(emptyList());

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfCheckDisabled() {

        service = new FalsePositiveCheckService(
            prisonApiClient,
            imageDuplicationDetectionService,
            DataComplianceProperties.builder().falsePositiveDuplicateCheckEnabled(false).build());

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfInsufficientNumberOfImagesToCompare() {

        mockImagesFor(REFERENCE_OFFENDER_NO, REFERENCE_OFFENDER_IMAGE_ID_1);
        mockImagesFor(DUPLICATE_OFFENDER_NO, DUPLICATE_OFFENDER_IMAGE_ID_1);

        lenient().when(imageDuplicationDetectionService.getSimilarity(any(), any()))
            .thenReturn(Optional.of(IMAGE_SIMILARITY_THRESHOLD - 1));

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    @Test
    void notFalsePositiveIfAnyImagePairSimilar() {

        final var referenceImages = mockImagesFor(REFERENCE_OFFENDER_NO, REFERENCE_OFFENDER_IMAGE_ID_1, REFERENCE_OFFENDER_IMAGE_ID_2);
        final var duplicateImages = mockImagesFor(DUPLICATE_OFFENDER_NO, DUPLICATE_OFFENDER_IMAGE_ID_1, DUPLICATE_OFFENDER_IMAGE_ID_2);

        when(imageDuplicationDetectionService.getSimilarity(any(), any()))
            .thenReturn(Optional.of(IMAGE_SIMILARITY_THRESHOLD - 1));

        when(imageDuplicationDetectionService.getSimilarity(referenceImages.get(0), duplicateImages.get(1)))
            .thenReturn(Optional.of(IMAGE_SIMILARITY_THRESHOLD + 1));

        assertThat(service.isFalsePositive(generateDataDuplicateCheck())).isFalse();
    }

    private RetentionCheckDataDuplicate generateDataDuplicateCheck() {
        final var check = new RetentionCheckDatabaseDataDuplicate(RETENTION_REQUIRED);
        check.addDataDuplicates(List.of(DataDuplicate.builder()
            .referenceOffenderNo(REFERENCE_OFFENDER_NO)
            .duplicateOffenderNo(DUPLICATE_OFFENDER_NO)
            .build()));
        return check;
    }

    private List<OffenderImage> mockImagesFor(final String offenderNo, final Long... imageIds) {
        when(prisonApiClient.getOffenderFaceImagesFor(new OffenderNumber(offenderNo)))
            .thenReturn(stream(imageIds).map(id -> new OffenderImageMetadata(id, "FACE")).collect(toList()));
        return stream(imageIds).map(id -> mockImageDataFor(offenderNo, id)).collect(toList());
    }

    private OffenderImage mockImageDataFor(final String offenderNumber, final long imageId) {
        final var offenderImage = mock(OffenderImage.class);
        lenient().when(prisonApiClient.getImageData(new OffenderNumber(offenderNumber), imageId))
            .thenReturn(Optional.of(offenderImage));
        return offenderImage;
    }
}
