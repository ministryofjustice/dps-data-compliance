package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

class ImageDuplicateRepositoryTest extends JpaRepositoryTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private OffenderImageUploadRepository offenderImageUploadRepository;

    @Autowired
    private ImageDuplicateRepository repository;

    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    void persistImageDuplicateAndRetrieveById() {

        final var imageDuplicate = ImageDuplicate.builder()
            .referenceOffenderImageUpload(offenderImageUploadRepository.findById(1L).orElseThrow())
            .duplicateOffenderImageUpload(offenderImageUploadRepository.findById(2L).orElseThrow())
            .detectionDateTime(DATE_TIME)
            .similarity(97.89)
            .build();

        repository.save(imageDuplicate);
        assertThat(imageDuplicate.getImageDuplicateId()).isNotNull();

        final var retrievedEntity = repository.findById(imageDuplicate.getImageDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getDetectionDateTime()).isEqualTo(DATE_TIME);
        assertThat(retrievedEntity.getReferenceOffenderImageUpload().getUploadId()).isEqualTo(1L);
        assertThat(retrievedEntity.getDuplicateOffenderImageUpload().getUploadId()).isEqualTo(2L);
        assertThat(retrievedEntity.getSimilarity()).isEqualTo(97.89);
    }

    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    void persistImageDuplicateWith100PercentSimilarity() {

        final var imageDuplicate = ImageDuplicate.builder()
            .referenceOffenderImageUpload(offenderImageUploadRepository.findById(1L).orElseThrow())
            .duplicateOffenderImageUpload(offenderImageUploadRepository.findById(2L).orElseThrow())
            .detectionDateTime(DATE_TIME)
            .similarity(100.00)
            .build();

        repository.save(imageDuplicate);
        assertThat(imageDuplicate.getImageDuplicateId()).isNotNull();

        final var retrievedEntity = repository.findById(imageDuplicate.getImageDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getSimilarity()).isEqualTo(100.00);
    }

    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    @Sql("classpath:seed.data/image_duplicate.sql")
    void findByOffenderImageUploadIds() {
        assertThat(repository.findByOffenderImageUploadIds(1L, 2L).orElseThrow().getImageDuplicateId()).isEqualTo(1L);
        assertThat(repository.findByOffenderImageUploadIds(2L, 1L).orElseThrow().getImageDuplicateId()).isEqualTo(1L);
    }

    @Test
    @Sql("classpath:seed.data/image_upload_batch.sql")
    @Sql("classpath:seed.data/offender_image_upload.sql")
    @Sql("classpath:seed.data/image_duplicate.sql")
    void findByOffenderImageUploadIdsReturnsEmpty() {
        assertThat(repository.findByOffenderImageUploadIds(111L, 222L)).isEmpty();
    }
}
