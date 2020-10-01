package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class ImageDuplicateRepositoryTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private OffenderImageUploadRepository offenderImageUploadRepository;

    @Autowired
    private ImageDuplicateRepository repository;

    @Test
    @Sql("image_upload_batch.sql")
    @Sql("offender_image_upload.sql")
    void persistImageDuplicateAndRetrieveById() {

        final var imageDuplicate = ImageDuplicate.builder()
                .referenceOffenderImageUpload(offenderImageUploadRepository.findById(1L).orElseThrow())
                .duplicateOffenderImageUpload(offenderImageUploadRepository.findById(2L).orElseThrow())
                .detectionDateTime(DATE_TIME)
                .similarity(97.89)
                .build();

        repository.save(imageDuplicate);
        assertThat(imageDuplicate.getImageDuplicateId()).isNotNull();

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById(imageDuplicate.getImageDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getDetectionDateTime()).isEqualTo(DATE_TIME);
        assertThat(retrievedEntity.getReferenceOffenderImageUpload().getUploadId()).isEqualTo(1L);
        assertThat(retrievedEntity.getDuplicateOffenderImageUpload().getUploadId()).isEqualTo(2L);
        assertThat(retrievedEntity.getSimilarity()).isEqualTo(97.89);
    }

    @Test
    @Sql("image_upload_batch.sql")
    @Sql("offender_image_upload.sql")
    void persistImageDuplicateWith100PercentSimilarity() {

        final var imageDuplicate = ImageDuplicate.builder()
                .referenceOffenderImageUpload(offenderImageUploadRepository.findById(1L).orElseThrow())
                .duplicateOffenderImageUpload(offenderImageUploadRepository.findById(2L).orElseThrow())
                .detectionDateTime(DATE_TIME)
                .similarity(100.00)
                .build();

        repository.save(imageDuplicate);
        assertThat(imageDuplicate.getImageDuplicateId()).isNotNull();

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById(imageDuplicate.getImageDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getSimilarity()).isEqualTo(100.00);
    }

    @Test
    @Sql("image_upload_batch.sql")
    @Sql("offender_image_upload.sql")
    @Sql("image_duplicate.sql")
    void findByOffenderImageUploadIds() {
        assertThat(repository.findByOffenderImageUploadIds(1L, 2L).orElseThrow().getImageDuplicateId()).isEqualTo(1L);
        assertThat(repository.findByOffenderImageUploadIds(2L, 1L).orElseThrow().getImageDuplicateId()).isEqualTo(1L);
    }

    @Test
    @Sql("image_upload_batch.sql")
    @Sql("offender_image_upload.sql")
    @Sql("image_duplicate.sql")
    void findByOffenderImageUploadIdsReturnsEmpty() {
        assertThat(repository.findByOffenderImageUploadIds(111L, 222L)).isEmpty();
    }
}
