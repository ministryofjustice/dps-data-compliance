package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;

import java.util.Optional;

@Repository
public interface ImageDuplicateRepository extends CrudRepository<ImageDuplicate, Long> {

    @Query(
        "SELECT i FROM ImageDuplicate i " +
            "WHERE i.referenceOffenderImageUpload.uploadId IN (:uploadId1, :uploadId2) " +
            "AND i.duplicateOffenderImageUpload.uploadId IN (:uploadId1, :uploadId2) " +
            "AND i.referenceOffenderImageUpload.uploadId != i.duplicateOffenderImageUpload.uploadId")
    Optional<ImageDuplicate> findByOffenderImageUploadIds(Long uploadId1, Long uploadId2);
}
