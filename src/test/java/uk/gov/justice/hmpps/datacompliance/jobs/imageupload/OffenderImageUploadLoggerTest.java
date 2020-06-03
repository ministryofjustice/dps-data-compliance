package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffenderImageUploadLoggerTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final FaceId FACE_ID = new FaceId("face1");
    private static final long IMAGE_ID = 123L;

    @Mock
    private OffenderImageUploadRepository repository;

    @Mock
    private ImageUploadBatch batch;

    private OffenderImageUploadLogger logger;

    @BeforeEach
    void setUp() {
        logger = new OffenderImageUploadLogger(repository, batch, TimeSource.of(DATE_TIME));
    }

    @Test
    void log() {

        assertThat(logger.getUploadCount()).isZero();

        when(repository.findByOffenderNoAndImageId(any(), any())).thenReturn(Optional.empty());
        logger.log(new OffenderNumber(OFFENDER_NUMBER), new OffenderImageMetadata(IMAGE_ID, "FACE"), FACE_ID);

        var offenderImageUpload = ArgumentCaptor.forClass(OffenderImageUpload.class);
        verify(repository).save(offenderImageUpload.capture());

        assertThat(offenderImageUpload.getValue().getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(offenderImageUpload.getValue().getImageId()).isEqualTo(IMAGE_ID);
        assertThat(offenderImageUpload.getValue().getFaceId()).isEqualTo(FACE_ID.getFaceId());
        assertThat(offenderImageUpload.getValue().getUploadDateTime()).isEqualTo(DATE_TIME);
        assertThat(offenderImageUpload.getValue().getImageUploadBatch()).isEqualTo(batch);

        assertThat(offenderImageUpload.getValue().getUploadErrorReason()).isNull();

        assertThat(logger.getUploadCount()).isEqualTo(1);
    }

    @Test
    void doesNotPersistUploadIfAlreadyExists() {

        assertThat(logger.getUploadCount()).isZero();

        when(repository.findByOffenderNoAndImageId(OFFENDER_NUMBER, IMAGE_ID))
                .thenReturn(Optional.of(mock(OffenderImageUpload.class)));

        logger.log(new OffenderNumber(OFFENDER_NUMBER), new OffenderImageMetadata(IMAGE_ID, "FACE"), FACE_ID);
        
        verify(repository, never()).save(any());
        assertThat(logger.getUploadCount()).isZero();
    }

    @Test
    void logUploadError() {

        assertThat(logger.getUploadCount()).isZero();

        when(repository.findByOffenderNoAndImageId(any(), any())).thenReturn(Optional.empty());
        logger.logUploadError(new OffenderNumber(OFFENDER_NUMBER), new OffenderImageMetadata(IMAGE_ID, "FACE"), "some reason");

        var offenderImageUpload = ArgumentCaptor.forClass(OffenderImageUpload.class);
        verify(repository).save(offenderImageUpload.capture());

        assertThat(offenderImageUpload.getValue().getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(offenderImageUpload.getValue().getImageId()).isEqualTo(IMAGE_ID);
        assertThat(offenderImageUpload.getValue().getUploadDateTime()).isEqualTo(DATE_TIME);
        assertThat(offenderImageUpload.getValue().getImageUploadBatch()).isEqualTo(batch);
        assertThat(offenderImageUpload.getValue().getUploadErrorReason()).isEqualTo("some reason");

        assertThat(offenderImageUpload.getValue().getFaceId()).isNull();

        assertThat(logger.getUploadCount()).isZero();
    }

}