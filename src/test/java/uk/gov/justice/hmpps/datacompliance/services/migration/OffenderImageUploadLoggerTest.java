package uk.gov.justice.hmpps.datacompliance.services.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.model.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OffenderImageUploadLoggerTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String OFFENDER_NUMBER = "offender1";
    private static final String FACE_ID = "face1";
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

        logger.log(new OffenderNumber(OFFENDER_NUMBER), new OffenderImageMetadata(IMAGE_ID, "FACE"), FACE_ID);

        var offenderImageUpload = ArgumentCaptor.forClass(OffenderImageUpload.class);
        verify(repository).save(offenderImageUpload.capture());

        assertThat(offenderImageUpload.getValue().getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(offenderImageUpload.getValue().getImageId()).isEqualTo(IMAGE_ID);
        assertThat(offenderImageUpload.getValue().getFaceId()).isEqualTo(FACE_ID);
        assertThat(offenderImageUpload.getValue().getUploadDateTime()).isEqualTo(DATE_TIME);
        assertThat(offenderImageUpload.getValue().getImageUploadBatch()).isEqualTo(batch);

        assertThat(logger.getUploadCount()).isEqualTo(1);
    }
}