package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch.BatchType;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeceasedOffenderDeletionBatchRepository extends CrudRepository<DeceasedOffenderDeletionBatch, Long> {

    Optional<DeceasedOffenderDeletionBatch> findFirstByBatchTypeOrderByRequestDateTimeDesc(BatchType batchType);

    List<DeceasedOffenderDeletionBatch> findByReferralCompletionDateTimeIsNull();
}
