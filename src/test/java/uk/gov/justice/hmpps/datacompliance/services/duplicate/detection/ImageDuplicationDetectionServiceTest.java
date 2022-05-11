package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceMatch;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload.OffenderImageUploadBuilder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.ImageDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload.ImageUploadStatus.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload.ImageUploadStatus.SUCCESS;

@ExtendWith(MockitoExtension.class)
class ImageDuplicationDetectionServiceTest {

    private static final long REFERENCE_ID = 0L;
    private static final long DUPLICATE_1 = 1L;
    private static final long DUPLICATE_2 = 2L;
    private static final long DUPLICATE_3 = 3L;
    private static final double SIMILARITY = 97.89;
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Mock
    private ImageRecognitionClient imageRecognitionClient;

    @Mock
    private OffenderImageUploadRepository offenderImageUploadRepository;

    @Mock
    private ImageDuplicateRepository imageDuplicateRepository;

    @Mock
    private ImageDuplicate duplicate1, duplicate2;

    private ImageDuplicationDetectionService service;

    @BeforeEach
    void setUp() {
        service = new ImageDuplicationDetectionService(
            imageRecognitionClient,
            offenderImageUploadRepository,
            imageDuplicateRepository,
            TimeSource.of(NOW));
    }

    @Test
    void findDuplicates() {

        givenOffenderHasAnUploaded(imageWith(REFERENCE_ID).build())
            .andImageRecognitionFindsMatchesWith(DUPLICATE_1, DUPLICATE_2, DUPLICATE_3)
            .andUploadedImagesExistForMatches(DUPLICATE_1, DUPLICATE_2)
            .andUploadedImageSharesSameOffenderNumberAsReference(DUPLICATE_3)
            .andDuplicateHasAlreadyBeenFound(DUPLICATE_1, duplicate1)
            .andDuplicateHasNotAlreadyBeenFound(DUPLICATE_2)
            .andCanSuccessfullyPersist(duplicate2);

        assertThat(service.findDuplicatesFor(offenderNo(REFERENCE_ID)))
            .containsExactlyInAnyOrder(duplicate1, duplicate2);

        verifyPersistedDuplicate(DUPLICATE_2);
    }

    @Test
    void findDuplicatesReturnsEmptyWhenNoImagesForOffender() {

        givenOffenderHasNoUploadedImages();

        assertThat(service.findDuplicatesFor(offenderNo(REFERENCE_ID))).isEmpty();

        verifyNoInteractions(imageDuplicateRepository);
    }

    @Test
    void findDuplicatesReturnsEmptyWhenImageUploadFailedForOffender() {

        givenOffenderHasAnUploaded(imageWith(REFERENCE_ID)
            .uploadErrorReason("Upload failed for some reason.")
            .build());

        assertThat(service.findDuplicatesFor(offenderNo(REFERENCE_ID))).isEmpty();

        verifyNoInteractions(imageDuplicateRepository);
    }

    @Test
    void findDuplicatesThrowsIfImageUploadRepositoryDoesNotContainFaceId() {

        givenOffenderHasAnUploaded(imageWith(REFERENCE_ID).build())
            .andImageRecognitionFindsMatchesWith(DUPLICATE_1)
            .andUploadedImagesExistForMatches(false, DUPLICATE_1);

        assertThatThrownBy(() -> service.findDuplicatesFor(offenderNo(REFERENCE_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot find image upload for faceId: 'face1'");

        verifyNoInteractions(imageDuplicateRepository);
    }

    @Test
    void deleteOffenderImages() {

        final var imageUpload = mock(OffenderImageUpload.class);
        when(imageUpload.getFaceId()).thenReturn("face1");
        when(offenderImageUploadRepository.findByOffenderNo("A1234AA")).thenReturn(List.of(imageUpload));

        service.deleteOffenderImages(new OffenderNumber("A1234AA"));

        final var inOrder = inOrder(imageRecognitionClient, imageUpload, offenderImageUploadRepository);
        inOrder.verify(imageRecognitionClient).removeFaceFromCollection(new FaceId("face1"));
        inOrder.verify(imageUpload).setUploadStatus(DELETED);
        inOrder.verify(offenderImageUploadRepository).save(imageUpload);
    }

    private ImageDuplicationDetectionServiceTest givenOffenderHasAnUploaded(final OffenderImageUpload image) {
        when(offenderImageUploadRepository.findByOffenderNo(offenderNo(REFERENCE_ID).getOffenderNumber()))
            .thenReturn(List.of(image));
        return this;
    }

    private void givenOffenderHasNoUploadedImages() {
        when(offenderImageUploadRepository.findByOffenderNo(offenderNo(REFERENCE_ID).getOffenderNumber()))
            .thenReturn(emptyList());
    }

    private ImageDuplicationDetectionServiceTest andImageRecognitionFindsMatchesWith(final long... duplicateIds) {
        when(imageRecognitionClient.findMatchesFor(faceId(REFERENCE_ID)))
            .thenReturn(stream(duplicateIds).mapToObj(id -> new FaceMatch(faceId(id), SIMILARITY)).collect(toSet()));
        return this;
    }

    private ImageDuplicationDetectionServiceTest andUploadedImagesExistForMatches(final long... duplicateIds) {
        return andUploadedImagesExistForMatches(true, duplicateIds);
    }

    private ImageDuplicationDetectionServiceTest andUploadedImagesExistForMatches(final boolean exists, final long... duplicateIds) {
        stream(duplicateIds).forEach(duplicateId ->
            when(offenderImageUploadRepository.findByFaceId(faceId(duplicateId).getFaceId()))
                .thenReturn(exists ? Optional.of(imageWith(duplicateId).build()) : Optional.empty()));
        return this;
    }

    private ImageDuplicationDetectionServiceTest andUploadedImageSharesSameOffenderNumberAsReference(final long duplicateId) {
        when(offenderImageUploadRepository.findByFaceId(faceId(duplicateId).getFaceId()))
            .thenReturn(Optional.of(
                OffenderImageUpload.builder()
                    .offenderNo(offenderNo(REFERENCE_ID).getOffenderNumber())
                    .build()));
        return this;
    }

    private ImageDuplicationDetectionServiceTest andDuplicateHasAlreadyBeenFound(final long duplicateUploadId, final ImageDuplicate duplicate) {
        when(imageDuplicateRepository.findByOffenderImageUploadIds(REFERENCE_ID, duplicateUploadId))
            .thenReturn(Optional.of(duplicate));
        return this;
    }

    private ImageDuplicationDetectionServiceTest andDuplicateHasNotAlreadyBeenFound(final long duplicateUploadId) {
        when(imageDuplicateRepository.findByOffenderImageUploadIds(REFERENCE_ID, duplicateUploadId))
            .thenReturn(Optional.empty());
        return this;
    }

    private void andCanSuccessfullyPersist(final ImageDuplicate duplicate) {
        when(imageDuplicateRepository.save(any())).thenReturn(duplicate);
    }

    private void verifyPersistedDuplicate(final long duplicateId) {
        final var persistedDuplicate = ArgumentCaptor.forClass(ImageDuplicate.class);
        verify(imageDuplicateRepository).save(persistedDuplicate.capture());
        assertThat(persistedDuplicate.getValue().getReferenceOffenderImageUpload().getUploadId()).isEqualTo(REFERENCE_ID);
        assertThat(persistedDuplicate.getValue().getDuplicateOffenderImageUpload().getUploadId()).isEqualTo(duplicateId);
        assertThat(persistedDuplicate.getValue().getDetectionDateTime()).isEqualTo(NOW);
        assertThat(persistedDuplicate.getValue().getSimilarity()).isEqualTo(SIMILARITY);
    }

    private OffenderImageUploadBuilder imageWith(final long id) {
        return OffenderImageUpload.builder()
            .uploadStatus(SUCCESS)
            .imageId(id)
            .uploadId(id)
            .faceId(faceId(id).getFaceId())
            .offenderNo(offenderNo(id).getOffenderNumber());
    }

    private FaceId faceId(final long suffix) {
        return new FaceId("face" + suffix);
    }

    private OffenderNumber offenderNo(final long index) {
        return new OffenderNumber(format("A%04dAA", index));
    }
}
