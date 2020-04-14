package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;

import java.util.Optional;

@Repository
public interface ManualRetentionRepository extends CrudRepository<ManualRetention, Long> {
    Optional<ManualRetention> findFirstByOffenderNoOrderByRetentionVersionDesc(final String offenderNo);
}
