package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;

@Repository
public interface RetentionCheckRepository extends CrudRepository<RetentionCheck, Long> {
}
