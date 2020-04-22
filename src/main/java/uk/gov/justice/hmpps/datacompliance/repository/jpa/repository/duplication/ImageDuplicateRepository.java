package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;

@Repository
public interface ImageDuplicateRepository extends CrudRepository<ImageDuplicate, Long> {
}
