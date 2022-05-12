package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload.ImageUploadStatus.ERROR;


@Sql(scripts = {"classpath:seed.data/reset.sql", "classpath:seed.data/image_upload_batch.sql", "classpath:seed.data/offender_image_upload.sql"})
class OffenderImageUploadRepositoryTest extends JpaRepositoryTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private ImageUploadBatchRepository batchRepository;

    @Autowired
    private OffenderImageUploadRepository uploadRepository;

    @Test
    void persistOffenderImageUploadAndRetrieveById() {

        final var offenderImageUpload = uploadRepository.save(buildOffenderImageUpload());
        assertThat(offenderImageUpload.getUploadId()).isNotNull();

        final var retrievedEntity = uploadRepository.findById(offenderImageUpload.getUploadId()).orElseThrow();
        assertThat(retrievedEntity.getOffenderNo()).isEqualTo("A1234AA");
        assertThat(retrievedEntity.getImageId()).isEqualTo(123L);
        assertThat(retrievedEntity.getFaceId()).isEqualTo("321");
        assertThat(retrievedEntity.getUploadDateTime()).isEqualTo(DATE_TIME);
        assertThat(retrievedEntity.getImageUploadBatch().getBatchId()).isEqualTo(1L);
        assertThat(retrievedEntity.getUploadStatus()).isEqualTo(ERROR);
        assertThat(retrievedEntity.getUploadErrorReason()).isEqualTo("Some error reason");
    }

    @Test
    void findOffenderImageUploadsByOffenderNo() {

        assertThat(uploadRepository.findByOffenderNo("OFFENDER1"))
            .extracting(OffenderImageUpload::getUploadId)
            .containsOnly(1L, 2L);

        assertThat(uploadRepository.findByOffenderNo("OFFENDER2"))
            .extracting(OffenderImageUpload::getUploadId)
            .containsOnly(3L);
    }

    @Test
    void findOffenderImageUploadByOffenderNoReturnsEmpty() {
        assertThat(uploadRepository.findByOffenderNo("UNKNOWN")).isEmpty();
    }

    @Test
    void findOffenderImageUploadByOffenderNoAndImageId() {
        assertThat(uploadRepository.findByOffenderNoAndImageId("OFFENDER1", 1L))
            .map(OffenderImageUpload::getUploadId)
            .contains(1L);
    }

    @Test
    void findOffenderImageUploadByOffenderNoAndImageIdReturnsEmpty() {
        assertThat(uploadRepository.findByOffenderNoAndImageId("UNKNOWN", 999L)).isEmpty();
    }

    @Test
    void findOffenderImageUploadByFaceId() {
        assertThat(uploadRepository.findByFaceId("1").orElseThrow().getUploadId())
            .isEqualTo(1L);
    }

    @Test
    void findOffenderImageUploadByFaceIdReturnsEmpty() {
        assertThat(uploadRepository.findByFaceId("UNKNOWN")).isEmpty();
    }

    private OffenderImageUpload buildOffenderImageUpload() {
        return OffenderImageUpload.builder()
            .offenderNo("A1234AA")
            .imageId(123L)
            .faceId("321")
            .uploadDateTime(DATE_TIME)
            .imageUploadBatch(batchRepository.findById(1L).orElseThrow())
            .uploadStatus(ERROR)
            .uploadErrorReason("Some error reason")
            .build();
    }
}
