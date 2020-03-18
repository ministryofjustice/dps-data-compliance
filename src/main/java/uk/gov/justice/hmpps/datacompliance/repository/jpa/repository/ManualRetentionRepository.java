package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.ManualRetention;

import java.util.Optional;

@Repository
public interface ManualRetentionRepository extends CrudRepository<ManualRetention, Long> {
    Optional<ManualRetention> findFirstByOffenderNoOrderByRetentionDateTimeDesc(final String offenderNo);
}
