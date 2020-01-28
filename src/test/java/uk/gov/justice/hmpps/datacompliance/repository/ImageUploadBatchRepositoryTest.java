package uk.gov.justice.hmpps.datacompliance.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch.ImageUploadBatchBuilder;

import javax.transaction.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ImageUploadBatchRepositoryTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final long BATCH_ID = 1L;

    @Autowired
    private ImageUploadBatchRepository repository;

    @Test
    void persistImageUploadBatchAndRetrieveById() {

        final var imageUploadBatch = buildImageUploadBatch()
                .uploadEndDateTime(DATE_TIME.plusSeconds(1))
                .uploadCount(123L)
                .build();

        repository.save(imageUploadBatch);
        assertThat(imageUploadBatch.getBatchId()).isNotNull();

        final var retrievedEntity = repository.findById(imageUploadBatch.getBatchId()).orElseThrow();
        assertThat(retrievedEntity.getUploadStartDateTime()).isEqualTo(DATE_TIME);
        assertThat(retrievedEntity.getUploadEndDateTime()).isEqualTo(DATE_TIME.plusSeconds(1));
        assertThat(retrievedEntity.getUploadCount()).isEqualTo(123L);
    }

    @Test
    @Sql(value = "image_upload_batch.sql")
    void nullableFieldsMayBeUpdated() {

        final var entityToUpdate = repository.findById(BATCH_ID).orElseThrow();
        entityToUpdate.setUploadCount(123L);
        entityToUpdate.setUploadEndDateTime(DATE_TIME.plusSeconds(1));

        repository.save(entityToUpdate);

        final var retrievedEntity = repository.findById(BATCH_ID).orElseThrow();
        assertThat(retrievedEntity.getUploadCount()).isEqualTo(123L);
        assertThat(retrievedEntity.getUploadEndDateTime()).isEqualTo(DATE_TIME.plusSeconds(1));
    }

    private ImageUploadBatchBuilder buildImageUploadBatch() {
        return ImageUploadBatch.builder().uploadStartDateTime(DATE_TIME);
    }
}