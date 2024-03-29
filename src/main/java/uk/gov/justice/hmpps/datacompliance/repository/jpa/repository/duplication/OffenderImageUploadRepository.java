package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;

import java.util.List;
import java.util.Optional;

@Repository
public interface OffenderImageUploadRepository extends CrudRepository<OffenderImageUpload, Long> {
    List<OffenderImageUpload> findByOffenderNo(String offenderNo);

    Optional<OffenderImageUpload> findByOffenderNoAndImageId(String offenderNo, Long imageId);

    Optional<OffenderImageUpload> findByFaceId(String faceId);
}
