package uk.gov.justice.hmpps.datacompliance.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;

@Repository
public interface ImageUploadBatchRepository extends CrudRepository<ImageUploadBatch, Long> {
}
