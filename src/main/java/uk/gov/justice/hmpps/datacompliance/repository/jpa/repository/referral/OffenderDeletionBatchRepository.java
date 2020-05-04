package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;

import java.util.List;
import java.util.Optional;

@Repository
public interface OffenderDeletionBatchRepository extends CrudRepository<OffenderDeletionBatch, Long> {
    Optional<OffenderDeletionBatch> findFirstByOrderByRequestDateTimeDesc();
    List<OffenderDeletionBatch> findByReferralCompletionDateTimeIsNull();
}
