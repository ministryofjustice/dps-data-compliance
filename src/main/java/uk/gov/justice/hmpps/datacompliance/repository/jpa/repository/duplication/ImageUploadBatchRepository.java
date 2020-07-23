package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;

import java.util.Optional;

@Repository
public interface ImageUploadBatchRepository extends CrudRepository<ImageUploadBatch, Long> {
    Optional<ImageUploadBatch> findFirstByBatchIdNotOrderByUploadStartDateTimeDesc(final long batchId);
}
