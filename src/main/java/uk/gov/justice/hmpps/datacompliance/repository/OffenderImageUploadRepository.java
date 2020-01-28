package uk.gov.justice.hmpps.datacompliance.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.model.OffenderImageUpload;

import java.util.List;

@Repository
public interface OffenderImageUploadRepository extends CrudRepository<OffenderImageUpload, Long> {
    List<OffenderImageUpload> findByOffenderNo(String offenderNo);
}
